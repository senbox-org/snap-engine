/*
 * $Id: CloudTopPressureOp.java,v 1.2 2007/03/30 15:11:20 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.s3tbx.meris.cloud;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jnn.Jnn;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.io.*;
import java.util.Calendar;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.2 $ $Date: 2007/03/30 15:11:20 $
 */
@OperatorMetadata(alias = "Meris.CloudTopPressureOp",
        version = "2.3.4",
        internal = true,
        authors = "Marco Zühlke",
        copyright = "(c) 2007-2009 by Brockmann Consult",
        description = "Computes cloud top pressure with FUB NN.")
public class CloudTopPressureOp extends MerisBasisOp {

    //    private static final String INVALID_EXPRESSION = "l1_flags.INVALID or not l1_flags.LAND_OCEAN";
    private static final String INVALID_EXPRESSION = "l1_flags.INVALID";
    private static final String LAND_EXPRESSION = "l1_flags.LAND_OCEAN";

    private static final String STRAYLIGHT_COEFF_FILE_NAME = "stray_ratio.d";
    private static final String STRAYLIGHT_CORR_WAVELENGTH_FILE_NAME = "lambda.d";

    private static final int BB760 = 10;
    private static final int DETECTOR_LENGTH_RR = 925;

    private float[] straylightCoefficients = new float[DETECTOR_LENGTH_RR]; // reduced resolution only!
    private float[] straylightCorrWavelengths = new float[DETECTOR_LENGTH_RR];


    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "If 'true' the algorithm will apply straylight correction.", defaultValue = "false")
    private boolean straylightCorr = false;

    private L2AuxData auxData;
    private L2CloudAuxData cloudAuxData;
    private ThreadLocal<JnnNet> waterNet;
    private ThreadLocal<JnnNet> landNet;

    private Band invalidBand;
    private RasterDataNode szaNode;
    private RasterDataNode saaNode;
    private RasterDataNode vzaNode;
    private RasterDataNode vaaNode;

    @Override
    public void initialize() throws OperatorException {
        try {
            loadNeuralNet();
        } catch (Exception e) {
            throw new OperatorException("Failed to load neural net ctp.nna:\n" + e.getMessage());
        }
        initAuxData();
        try {
            if (straylightCorr) {
                readStraylightCoeff();
                readStraylightCorrWavelengths();
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to load straylight correction auxdata:\n" + e.getMessage());
        }
        createTargetProduct();

        szaNode = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        saaNode = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME);
        vzaNode = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME);
        vaaNode = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME);

    }

    private void loadNeuralNet() throws IOException, JnnException {
        final File neuralNetLandFile = new File(getClass().getResource("ctp_NN_1.nna").getPath());
        final File neuralNetWaterFile = new File(getClass().getResource("ctp_NN_2.nna").getPath());
        final JnnNet neuralNetLand = readNeuralNet(neuralNetLandFile);
        final JnnNet neuralNetWater = readNeuralNet(neuralNetWaterFile);
        landNet = new ThreadLocal<JnnNet>() {
            @Override
            protected JnnNet initialValue() {
                return neuralNetLand.clone();
            }
        };
        waterNet = new ThreadLocal<JnnNet>() {
            @Override
            protected JnnNet initialValue() {
                return neuralNetWater.clone();
            }
        };
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER_CTP", "MER_L2");
        if (straylightCorr) {
            targetProduct.addBand("cloud_top_press_straylight_corr", ProductData.TYPE_FLOAT32);
        } else {
            targetProduct.addBand("cloud_top_press", ProductData.TYPE_FLOAT32);
        }
        invalidBand = createBooleanExpressionBand(INVALID_EXPRESSION, sourceProduct);
    }

    private void initAuxData() throws OperatorException {
        try {
            L2AuxDataProvider auxdataProvider = L2AuxDataProvider.getInstance();
            auxData = auxdataProvider.getAuxdata(sourceProduct);
            int month = sourceProduct.getStartTime().getAsCalendar().get(Calendar.MONTH);
            cloudAuxData = new L2CloudAuxData(auxdataProvider.getDpmConfig(), month);
        } catch (Exception e) {
            throw new OperatorException("Failed to load L2AuxData:\n" + e.getMessage(), e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle rectangle = targetTile.getRectangle();

        Tile detector = getSourceTile(sourceProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME), rectangle);

        Tile sza = getSourceTile(szaNode, rectangle);
        Tile saa = getSourceTile(saaNode, rectangle);
        Tile vza = getSourceTile(vzaNode, rectangle);
        Tile vaa = getSourceTile(vaaNode, rectangle);


        Tile toar10 = getSourceTile(sourceProduct.getBand("radiance_10"), rectangle);
        Tile toar11 = getSourceTile(sourceProduct.getBand("radiance_11"), rectangle);

        Tile isInvalid = getSourceTile(invalidBand, rectangle);

        Tile l1bFlags = getSourceTile(sourceProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME), rectangle);

        final double[] nnInWater = new double[6];
        final double[] nnInLand = new double[7];
        final double[] nnOut = new double[1];

        JnnNet nnLand = landNet.get();
        JnnNet nnWater = waterNet.get();

        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            checkForCancellation();
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                if (isInvalid.getSampleBoolean(x, y)) {
                    targetTile.setSample(x, y, 0);
                } else {

                    double szaRad = sza.getSampleFloat(x, y) * MathUtils.DTOR;
                    double vzaRad = vza.getSampleFloat(x, y) * MathUtils.DTOR;

                    double stray = 0.0;
                    double lambda = auxData.central_wavelength[BB760][detector.getSampleInt(x, y)];
                    if (straylightCorr) {
                        // apply FUB straylight correction...
                        stray = straylightCoefficients[detector.getSampleInt(x, y)] * toar10.getSampleDouble(x, y);
                        lambda = straylightCorrWavelengths[detector.getSampleInt(x, y)];
                    }

                    final double toar11XY_corrected = toar11.getSampleDouble(x, y) + stray;

                    if (l1bFlags.getSampleBit(x, y, Constants.L1_F_LAND)) {
                        final GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(new PixelPos(x, y), null);
                        nnInLand[0] = computeSurfAlbedo((float) geoPos.getLat(), (float) geoPos.getLon()); // albedo
                        nnInLand[1] = toar10.getSampleDouble(x, y);
                        nnInLand[2] = toar11XY_corrected / toar10.getSampleDouble(x, y);
                        nnInLand[3] = Math.cos(szaRad);
                        nnInLand[4] = Math.cos(vzaRad);
                        nnInLand[5] = Math.sin(vzaRad) *
                                Math.cos(MathUtils.DTOR * (vaa.getSampleFloat(x, y) - saa.getSampleFloat(x, y)));
                        nnInLand[6] = lambda;

                        nnLand.process(nnInLand, nnOut);
                    } else {
                        nnInWater[0] = toar10.getSampleDouble(x, y);
                        nnInWater[1] = toar11XY_corrected / toar10.getSampleDouble(x, y);
                        nnInWater[2] = Math.cos(szaRad);
                        nnInWater[3] = Math.cos(vzaRad);
                        nnInWater[4] = Math.sin(vzaRad) *
                                Math.cos(MathUtils.DTOR * (vaa.getSampleFloat(x, y) - saa.getSampleFloat(x, y)));
                        nnInWater[5] = lambda;

                        nnWater.process(nnInWater, nnOut);
                    }
                    targetTile.setSample(x, y, nnOut[0]);
                }
            }
        }
    }

    private static Band createBooleanExpressionBand(String expression, Product sourceProduct) {
        final BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "band1";
        bandDescriptor.expression = expression;
        bandDescriptor.type = ProductData.TYPESTRING_INT8;

        BandMathsOp bandMathsOp = new BandMathsOp();
        bandMathsOp.setParameterDefaultValues();
        bandMathsOp.setSourceProduct(sourceProduct);
        bandMathsOp.setTargetBandDescriptors(bandDescriptor);
        return bandMathsOp.getTargetProduct().getBandAt(0);
    }


    private double computeSurfAlbedo(float latitude, float longitude) {
// if ( (cloudAuxData.surfAlb.Surfalb.tab2[0] >= 0.0) && pPixel->lon < 0.0) lon
// += 360.0;
        /* a priori tab values in 0-360 deg */
        /* Note that it is also assumed that longitude tab values are increasing */
        FractIndex[] SaIndex = FractIndex.createArray(2);
        Interp.interpCoord(latitude, cloudAuxData.surfAlb.getTab(0), SaIndex[0]);
        Interp.interpCoord(longitude, cloudAuxData.surfAlb.getTab(1), SaIndex[1]);

        /* 	if ( weight[0] > 0.5 ) index[0]++; v 4.3- align with DPM */
        /* 	if ( weight[1] > 0.5 ) index[1]++; */

        /* DPM #2.1.5-1 */
        /* 	*pfSA = Surfalb.LUT[index[0]][index[1]]; v4.3- align with DPM */
        return Interp.interpolate(cloudAuxData.surfAlb.getJavaArray(), SaIndex);
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
        final InputStream inputStream = CloudTopPressureOp.class.getResourceAsStream(fileName);
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

    private JnnNet readNeuralNet(File nnFile) throws IOException, JnnException {
        final InputStreamReader reader1 = new FileReader(nnFile);
        try {
            Jnn.setOptimizing(true);
            return Jnn.readNna(reader1);
        } finally {
            reader1.close();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CloudTopPressureOp.class);
        }
    }
}
