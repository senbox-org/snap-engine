/*
 * $Id: CloudProbabilityOp.java,v 1.2 2007/04/25 14:15:31 marcoz Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
import org.esa.s3tbx.meris.AlbedoUtils;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * A processing node to compute a cloud_probability mask using a neural network.
 */
@OperatorMetadata(alias = "Meris.CloudProbability", internal = true)
public class CloudProbabilityOp extends MerisBasisOp {

    public static final String CLOUD_AUXDATA_DIR_PROPERTY = "cloud.auxdata.dir";

    public static final String CONFIG_FILE_NAME = "config_file_name";
    public static final String INVALID_EXPRESSION = "invalid_expression";

    public static final String CLOUD_PROP_BAND = "cloud_prob";
    public static final String CLOUD_FLAG_BAND = "cloud_flag";
    
    public static final int FLAG_INVALID = 0;
    public static final int FLAG_CLOUDY = 1;
    public static final int FLAG_CLOUDFREE = 2;
    public static final int FLAG_UNCERTAIN = 4;

    private static final String DEFAULT_OUTPUT_PRODUCT_NAME = "MER_CLOUD";
    private static final String PRODUCT_TYPE = "MER_L2_CLOUD";

    private static final String DEFAULT_CONFIG_FILE = "cloud_config.txt";
    private static final String DEFAULT_VALID_LAND_EXP = "not l1_flags.INVALID and dem_alt > -50";
    private static final String DEFAULT_VALID_OCEAN_EXP = "not l1_flags.INVALID and dem_alt <= -50";
    private static final float SCALING_FACTOR = 0.001f;

    private static final String PRESS_SCALE_HEIGHT_KEY = "press_scale_height";

    private float[] centralWavelenth;
    private CentralWavelengthProvider centralWavelengthProvider;

    private Band cloudBand;
    private Band cloudFlagBand;
    private Band[] radianceBands;
    
    private Band validLandBand;
	private Band validOceanBand;
	private Band landBand;

    private CloudAlgorithm landAlgo;
    private CloudAlgorithm oceanAlgo;
    /**
     * Pressure scale height to account for altitude.
     */
    private int pressScaleHeight;

    @SourceProduct(alias="input")
    private Product l1bProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String configFile = DEFAULT_CONFIG_FILE;
    @Parameter
    private String validLandExpression = DEFAULT_VALID_LAND_EXP;
    @Parameter
    private String validOceanExpression = DEFAULT_VALID_OCEAN_EXP;

    private File auxdataTargetDir;
    private Properties configProperties;


    @Override
    public void initialize() throws OperatorException {
        try {
            loadAuxdata();
        } catch (IOException e) {
            throw new OperatorException("Could not load auxdata", e);
        }
        
        centralWavelenth = centralWavelengthProvider.getCentralWavelength(l1bProduct.getProductType());

        // create the output product
        targetProduct = createCompatibleProduct(l1bProduct, DEFAULT_OUTPUT_PRODUCT_NAME, PRODUCT_TYPE);

        cloudBand = targetProduct.addBand(CLOUD_PROP_BAND, ProductData.TYPE_INT16);
        cloudBand.setDescription("Probability of clouds");
        cloudBand.setScalingFactor(SCALING_FACTOR);
        cloudBand.setNoDataValueUsed(true);
        cloudBand.setGeophysicalNoDataValue(-1);

        // create and add the flags coding
        FlagCoding cloudFlagCoding = createCloudFlagCoding(targetProduct);
        targetProduct.getFlagCodingGroup().add(cloudFlagCoding);

        // create and add the SDR flags dataset
        cloudFlagBand = targetProduct.addBand(CLOUD_FLAG_BAND, ProductData.TYPE_UINT8);
        cloudFlagBand.setDescription("Cloud specific flags");
        cloudFlagBand.setSampleCoding(cloudFlagCoding);
        
        String[] radianceBandNames = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;
        radianceBands = new Band[radianceBandNames.length];
        
        for (int bandIndex = 0; bandIndex < radianceBandNames.length; bandIndex++) {
            String bandName = radianceBandNames[bandIndex];
            radianceBands[bandIndex] = l1bProduct.getBand(bandName);
            if (radianceBands[bandIndex] == null) {
                throw new IllegalArgumentException("Source product does not contain band " + bandName);
            }
        }
        createBooleanBands();
    }
    
    private void createBooleanBands() throws OperatorException {
    	
		Map<String, Object> parameters = new HashMap<>();
		BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[3];
		
		bandDescriptors[0] = new BandMathsOp.BandDescriptor();
		bandDescriptors[0].name = "validLand";
		bandDescriptors[0].expression = validLandExpression;
		bandDescriptors[0].type = ProductData.TYPESTRING_INT8;
		
		bandDescriptors[1] = new BandMathsOp.BandDescriptor();
		bandDescriptors[1].name = "validOcean";
		bandDescriptors[1].expression = validOceanExpression;
		bandDescriptors[1].type = ProductData.TYPESTRING_INT8;
		
		bandDescriptors[2] = new BandMathsOp.BandDescriptor();
		bandDescriptors[2].name = "land";
		bandDescriptors[2].expression = "l1_flags.LAND_OCEAN";
		bandDescriptors[2].type = ProductData.TYPESTRING_INT8;
		
		parameters.put("targetBands", bandDescriptors);

		Product expProduct = GPF.createProduct("BandMaths", parameters, l1bProduct);

		validLandBand = expProduct.getBand("validLand");
		validOceanBand = expProduct.getBand("validOcean");
		landBand = expProduct.getBand("land");
	}

    private void loadAuxdata() throws IOException {
//        String auxdataSrcPath = "auxdata/cloudprob";
//        final String auxdataDestPath = ".beam/" + AlbedomapConstants.SYMBOLIC_NAME +
//                "/" + auxdataSrcPath;
//        auxdataTargetDir = new File(SystemUtils.getUserHomeDir(), auxdataDestPath);
//        URL sourceUrl = ResourceInstaller.getSourceUrl(this.getClass());
//
//        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceUrl, auxdataSrcPath, auxdataTargetDir);
//        resourceInstaller.install(".*", new NullProgressMonitor());

        // todo: clarify if this is the right way in Snap/S3tbx:
        final Path auxdataDirPath = SystemUtils.getAuxDataPath().resolve("cloudprob").toAbsolutePath();
        auxdataTargetDir = auxdataDirPath.toFile();
        final Path sourcePath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata");
        try {
            new ResourceInstaller(sourcePath, auxdataDirPath).install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final File configPropFile = new File(auxdataTargetDir, configFile);
        final InputStream propertiesStream = new FileInputStream(configPropFile);
        configProperties = new Properties();
        configProperties.load(propertiesStream);

        pressScaleHeight = Integer.parseInt(configProperties
                .getProperty(PRESS_SCALE_HEIGHT_KEY));

        centralWavelengthProvider = new CentralWavelengthProvider();
        centralWavelengthProvider.readAuxData(auxdataTargetDir);

        try {
            landAlgo = new CloudAlgorithm(auxdataTargetDir, configProperties.getProperty("land"));
            oceanAlgo = new CloudAlgorithm(auxdataTargetDir, configProperties.getProperty("ocean"));
        } catch (IOException e) {
            throw new OperatorException("Could not load auxdata", e);
        }

    }

    public static FlagCoding createCloudFlagCoding(Product outputProduct) {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(CLOUD_FLAG_BAND);
        flagCoding.setDescription("Cloud Flag Coding");

        int index = 0;
        int w = outputProduct.getSceneRasterWidth();
        int h = outputProduct.getSceneRasterHeight();
        Mask mask;

        cloudAttr = new MetadataAttribute("cloudy", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUDY);
        cloudAttr.setDescription("is with more than 80% cloudy");
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(1, 3), 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);


        cloudAttr = new MetadataAttribute("cloudfree", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUDFREE);
        cloudAttr.setDescription("is with less than 20% cloudy");
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(2, 3), 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);

        cloudAttr = new MetadataAttribute("cloud_uncertain", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_UNCERTAIN);
        cloudAttr.setDescription("is with between 20% and 80% cloudy");
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(3, 3), 0.5f);
        outputProduct.getMaskGroup().add(index, mask);

        return flagCoding;
    }

    /**
     * Creates a new color object to be used in the bitmaskDef.
     * The given indices start with 1.
     *
     */
    private static Color createBitmaskColor(int index, int maxIndex) {
        final double rf1 = 0.0;
        final double gf1 = 0.5;
        final double bf1 = 1.0;

        final double a = 2 * Math.PI * index / maxIndex;

        return new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {

        pm.beginTask("Processing frame...", rectangle.height);

        try {
        	//sources
        	Tile[] radiance = new Tile[radianceBands.length];
            for (int i1 = 0; i1 < radianceBands.length; i1++) {
			    radiance[i1] = getSourceTile(radianceBands[i1], rectangle);
			}
			
            Tile detector = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME), rectangle);
            Tile sza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), rectangle);
            Tile saa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME), rectangle);
            Tile vza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), rectangle);
            Tile vaa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME), rectangle);
            Tile pressure = getSourceTile(l1bProduct.getTiePointGrid("atm_press"), rectangle);
            Tile altitude = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME), rectangle);
			
            Tile isValidLand = getSourceTile(validLandBand, rectangle);
            Tile isValidOcean = getSourceTile(validOceanBand, rectangle);
            Tile isLand = getSourceTile(landBand, rectangle);
			
			//targets
            Tile cloudTile = targetTiles.get(cloudBand);
            Tile flagTile = targetTiles.get(cloudFlagBand);
            CloudAlgorithm cloneLandAlgo = null;
            CloudAlgorithm cloneOceanAlgo = null;
            try {
                cloneLandAlgo = landAlgo.clone();
                cloneOceanAlgo = oceanAlgo.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            final double[] cloudIn = new double[15];
			for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
				for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
					if (pm.isCanceled()) {
						break;
					}
					flagTile.setSample(x, y, 0);
					if (!isValidLand.getSampleBoolean(x, y) && !isValidOcean.getSampleBoolean(x, y)) {
						cloudTile.setSample(x, y, -1);
					} else {
						final double aziDiff = AlbedoUtils
								.computeAzimuthDifference(vaa.getSampleFloat(x, y), saa.getSampleFloat(x, y))
								* MathUtils.DTOR;
						final double szaCos = Math.cos(sza.getSampleFloat(x, y) * MathUtils.DTOR);
						cloudIn[0] = calculateI(radiance[0].getSampleDouble(x, y),
								radianceBands[0].getSolarFlux(), szaCos);
						cloudIn[1] = calculateI(radiance[1].getSampleDouble(x, y),
								radianceBands[1].getSolarFlux(), szaCos);
						cloudIn[2] = calculateI(radiance[2].getSampleDouble(x, y),
								radianceBands[2].getSolarFlux(), szaCos);
						cloudIn[3] = calculateI(radiance[3].getSampleDouble(x, y),
								radianceBands[3].getSolarFlux(), szaCos);
						cloudIn[4] = calculateI(radiance[4].getSampleDouble(x, y),
								radianceBands[4].getSolarFlux(), szaCos);
						cloudIn[5] = calculateI(radiance[5].getSampleDouble(x, y),
								radianceBands[5].getSolarFlux(), szaCos);
						cloudIn[6] = calculateI(radiance[8].getSampleDouble(x, y),
								radianceBands[8].getSolarFlux(), szaCos);
						cloudIn[7] = calculateI(radiance[9].getSampleDouble(x, y),
								radianceBands[9].getSolarFlux(), szaCos);
						cloudIn[8] = calculateI(radiance[12].getSampleDouble(x, y),
								radianceBands[12].getSolarFlux(), szaCos);
						cloudIn[9] = (radiance[10].getSampleDouble(x, y) * radianceBands[9]
								.getSolarFlux())
								/ (radiance[9].getSampleDouble(x, y) * radianceBands[10]
										.getSolarFlux());
						cloudIn[10] = altitudeCorrectedPressure(pressure.getSampleFloat(x, y),
								altitude.getSampleFloat(x, y), isLand.getSampleBoolean(x, y));
						cloudIn[11] = centralWavelenth[detector.getSampleInt(x, y)]; // central-wavelength
																		// channel
																		// 11
						cloudIn[12] = szaCos;
						final double vzaRad = vza.getSampleFloat(x, y) * MathUtils.DTOR;
						cloudIn[13] = Math.cos(vzaRad);
						cloudIn[14] = Math.cos(aziDiff) * Math.sin(vzaRad);

						double cloudProbability = 0;
						if (isValidLand.getSampleBoolean(x, y)) {
							cloudProbability = cloneLandAlgo.computeCloudProbability(cloudIn);
						} else if (isValidOcean.getSampleBoolean(x, y)) {
							cloudProbability = cloneOceanAlgo.computeCloudProbability(cloudIn);
						}

						if (cloudProbability > 0.8) {
							flagTile.setSample(x, y, FLAG_CLOUDY);
						} else if (cloudProbability < 0.2) {
							flagTile.setSample(x, y, FLAG_CLOUDFREE);
						} else if (cloudProbability >= 0.2
								&& cloudProbability <= 0.8) {
							flagTile.setSample(x, y, FLAG_UNCERTAIN);
						}
						cloudTile.setSample(x, y, cloudProbability);
					}
				}
				pm.worked(1);
			}
        } finally {
            pm.done();
        }
    }

    protected double calculateI(double rad, float sunSpectralFlux, double sunZenithCos) {
        return (rad / (sunSpectralFlux * sunZenithCos));
    }

    protected double altitudeCorrectedPressure(double press, double alt, boolean isLandPixel) {
        double correctedPressure;
        if (isLandPixel) {
            // ECMWF pressure is only corrected for positive altitudes and only
			// for land pixels */
            double f = Math.exp(-Math.max(0.0, alt) / pressScaleHeight);
            correctedPressure = press * f;
        } else {
            correctedPressure = press;
        }
        return correctedPressure;
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CloudProbabilityOp.class);
        }
    }
}
