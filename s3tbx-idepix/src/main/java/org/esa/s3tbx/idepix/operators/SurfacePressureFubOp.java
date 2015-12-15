/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jnn.Jnn;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Operator for computing surface pressure with optional straylight correction (FUB NN algorithm).
 *
 * @author Olaf Danne
 * @version $Revision: 6676 $ $Date: 2009-10-27 16:57:46 +0100 (Di, 27 Okt 2009) $
 */
@OperatorMetadata(alias = "idepix.operators.SurfacePressureFub",
                  version = "2.2",
                  internal = true,
                  authors = "Olaf Danne",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "This operator computes surface pressure with FUB NN algorithm.")
public class SurfacePressureFubOp extends MerisBasisOp {

    @SourceProduct(alias = "l1b", description = "The source product.")
    Product sourceProduct;
    @SourceProduct(alias = "cloud")
    private Product cloudProduct;
    @TargetProduct(description = "The target product.")
    Product targetProduct;
    @Parameter(description = "If 'true' the algorithm will apply straylight correction.", defaultValue = "false")
    public boolean straylightCorr = false;
    @Parameter(description = "If 'true' the algorithm will apply Tropical instead of USS atmosphere model.",
               defaultValue = "false")
    public boolean tropicalAtmosphere = false;

    //    private static final String INVALID_EXPRESSION = "l1_flags.INVALID or not l1_flags.LAND_OCEAN";
    private static final String INVALID_EXPRESSION = "l1_flags.INVALID";

    private static final String NEURAL_NET_TRP_FILE_NAME = "SP_FUB_trp.nna";
    private static final String NEURAL_NET_USS_FILE_NAME = "SP_FUB_uss.nna";  // changed to US standard atm., 18/03/2009
    private static final String STRAYLIGHT_COEFF_FILE_NAME = "stray_ratio.d";
    private static final String STRAYLIGHT_CORR_WAVELENGTH_FILE_NAME = "lambda.d";

    private static final int BB760 = 10;
    private static final int DETECTOR_LENGTH_RR = 925;

    private float[] straylightCoefficients = new float[DETECTOR_LENGTH_RR]; // reduced resolution only!
    private float[] straylightCorrWavelengths = new float[DETECTOR_LENGTH_RR];

    private L2AuxData auxData;
    private VirtualBandOpImage invalidImage;
    private ThreadLocal<JnnNet> neuralNet;


    @Override
    public void initialize() throws OperatorException {
        try {
            initAuxData();
            readStraylightCoeff();
            readStraylightCorrWavelengths();
        } catch (Exception e) {
            throw new OperatorException("Failed to load aux data:\n" + e.getMessage());
        }
        try {
            final JnnNet nn = loadNeuralNet();
            neuralNet = new ThreadLocal<JnnNet>() {
                @Override
                protected JnnNet initialValue() {
                    return nn.clone();
                }
            };
        } catch (Exception e) {
            throw new OperatorException("Failed to load neural net:\n" + e.getMessage());
        }
        createTargetProduct();
    }

    private JnnNet loadNeuralNet() throws IOException, JnnException {

        InputStream inputStream;
        if (tropicalAtmosphere) {
            inputStream = SurfacePressureFubOp.class.getResourceAsStream(NEURAL_NET_TRP_FILE_NAME);
        } else {
            inputStream = SurfacePressureFubOp.class.getResourceAsStream(NEURAL_NET_USS_FILE_NAME);
        }
        final InputStreamReader reader = new InputStreamReader(inputStream);

        try {
            Jnn.setOptimizing(true);
            return Jnn.readNna(reader);
        } finally {
            reader.close();
        }
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER_PSURF_NN", "MER_L2");
        targetProduct.addBand("surface_press_fub", ProductData.TYPE_FLOAT32);

        invalidImage = VirtualBandOpImage.builder(INVALID_EXPRESSION, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();
    }

    private void initAuxData() throws Exception {
        try {
            L2AuxDataProvider auxdataProvider = L2AuxDataProvider.getInstance();
            auxData = auxdataProvider.getAuxdata(sourceProduct);
        } catch (Exception e) {
            throw new OperatorException("Failed to load L2AuxData:\n" + e.getMessage(), e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {


        try {
            Rectangle rectangle = targetTile.getRectangle();
            JnnNet jnnNet = neuralNet.get();

            Tile detector = getSourceTile(sourceProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME),
                                          rectangle);
            Tile sza = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                                     rectangle);
            Tile saa = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                                     rectangle);
            Tile vza = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                                     rectangle);
            Tile vaa = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                     rectangle);

            Band band10 = sourceProduct.getBand("radiance_10");
            Tile toar10 = getSourceTile(band10, rectangle);
            Band band11 = sourceProduct.getBand("radiance_11");
            Tile toar11 = getSourceTile(band11, rectangle);
            Band band12 = sourceProduct.getBand("radiance_12");
            Tile toar12 = getSourceTile(band12, rectangle);

            final Raster isInvalid = invalidImage.getData(rectangle);

            final double[] nnIn = new double[7];
            final double[] nnOut = new double[1];

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (isInvalid.getSample(x, y, 0) != 0) {
                        targetTile.setSample(x, y, 0);
                    } else {
                        final int detectorXY = detector.getSampleInt(x, y);
                        final float szaDeg = sza.getSampleFloat(x, y);
                        final double szaRad = szaDeg * MathUtils.DTOR;
                        final float vzaDeg = vza.getSampleFloat(x, y);
                        final double vzaRad = vzaDeg * MathUtils.DTOR;

                        double lambda = auxData.central_wavelength[BB760][detectorXY];
                        final double fraction = (lambda - 753.75) / (778.0 - 753.75);
                        final double toar10XY = toar10.getSampleDouble(x, y) / band10.getSolarFlux();
                        final double toar11XY = toar11.getSampleDouble(x, y) / band11.getSolarFlux();
                        final double toar12XY = toar12.getSampleDouble(x, y) / band12.getSolarFlux();
                        final double toar11XY_na = (1.0 - fraction) * toar10XY + fraction * toar12XY;

                        double stray = 0.0;
                        if (straylightCorr) {
                            // apply FUB straylight correction...
                            stray = straylightCoefficients[detectorXY] * toar10XY;
                            lambda = straylightCorrWavelengths[detectorXY];
                        }

                        final double toar11XY_corrected = toar11XY + stray;

                        // apply FUB NN...
                        nnIn[0] = toar10XY;
                        nnIn[1] = toar11XY_corrected / toar11XY_na;
                        nnIn[2] = 0.15; // AOT
                        nnIn[3] = Math.cos(szaRad);
                        nnIn[4] = Math.cos(vzaRad);
                        final float vaaDegXY = vaa.getSampleFloat(x, y);
                        final float saaDegXY = saa.getSampleFloat(x, y);
                        nnIn[5] = Math.sin(vzaRad) * Math.cos(MathUtils.DTOR * (vaaDegXY - saaDegXY));
                        nnIn[6] = lambda;

                        jnnNet.process(nnIn, nnOut);
                        targetTile.setSample(x, y, nnOut[0]);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to process Surface Pressure FUB:\n" + e.getMessage(), e);
        }
    }

    /*
     * This method reads the straylight correction coefficients (RR only!)
     */
    private void readStraylightCoeff() throws IOException {
        readAuxdataArray(STRAYLIGHT_COEFF_FILE_NAME, straylightCoefficients);
    }

    /*
     * This method reads the straylight correction wavelengths (RR only!)
     */
    private void readStraylightCorrWavelengths() throws IOException {
        readAuxdataArray(STRAYLIGHT_CORR_WAVELENGTH_FILE_NAME, straylightCorrWavelengths);
    }

    private void readAuxdataArray(String fileName, float[] array) throws IOException {
        final InputStream inputStream = SurfacePressureFubOp.class.getResourceAsStream(fileName);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            for (int i = 0; i < array.length; i++) {
                String line = bufferedReader.readLine();
                line = line.trim();
                array[i] = Float.parseFloat(line);
            }
        } finally {
            bufferedReader.close();
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SurfacePressureFubOp.class);
        }
    }
}
