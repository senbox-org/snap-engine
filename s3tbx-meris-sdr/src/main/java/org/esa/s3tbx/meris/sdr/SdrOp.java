package org.esa.s3tbx.meris.sdr;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jnn.Jnn;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.s3tbx.meris.AlbedoUtils;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

@OperatorMetadata(alias = "Meris.Sdr", internal = true)
public class SdrOp extends MerisBasisOp {

    private static final String DEFAULT_OUTPUT_PRODUCT_NAME = "MER_SDR";
    private static final String SDR_PRODUCT_TYPE = "MER_L2_SDR";

    private static final String SDR_BAND_NAME_PREFIX = "sdr_";
    private static final String SDR_FLAGS_BAND_NAME = "sdr_flags";

    private static final String SDR_INVALID_FLAG_NAME = "INVALID_SDR";
    private static final int SDR_INVALID_FLAG_VALUE = 1;
    private static final float SCALING_FACTOR = 0.0001f;

    private static final int[] sdrBandNo = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14};

    private SdrAlgorithm algorithm;
    private Band[] reflectanceBands;
    private Band[] sdrBands;
    private Band sdrFlagBand;
    private Band validBand;

    @SourceProduct(alias="l1b")
    private Product l1bProduct;
    @SourceProduct(alias="brr")
    private Product brrProduct;
    @SourceProduct(alias="aerosol")
    private Product aerosolProduct;
    @SourceProduct(alias="mask")
    private Product maskProduct;//for expression only
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String neuralNetFile;
    @Parameter
    private String validBandName;
    @Parameter
    private String aot470Name;
    @Parameter
    private String angName;
    @Parameter
    private double angValue;
	
    @Override
    public void initialize() throws OperatorException {
        if (StringUtils.isNullOrEmpty(neuralNetFile)) {
            throw new OperatorException("No neural net specified.");
        }
        if (StringUtils.isNullOrEmpty(aot470Name)) {
            throw new OperatorException("No aot470 band specified.");
        }
        try {
            loadNeuralNet();
        } catch (Exception e) {
            throw new OperatorException("Failed to load neural net " + neuralNetFile + ":\n" + e.getMessage());
        }
        createTargetProduct();
    }

    private void createTargetProduct() {
        targetProduct = createCompatibleProduct(l1bProduct, DEFAULT_OUTPUT_PRODUCT_NAME, SDR_PRODUCT_TYPE);
        
        reflectanceBands = new Band[sdrBandNo.length];
        sdrBands = new Band[sdrBandNo.length];
        for (int i = 0; i < sdrBandNo.length; i++) {
        	reflectanceBands[i] = brrProduct.getBand("brr_" + Integer.toString(sdrBandNo[i]));
        	
            final Band band = l1bProduct.getBand("radiance_" + Integer.toString(sdrBandNo[i]));

            final Band sdrOutputBand = targetProduct.addBand(SDR_BAND_NAME_PREFIX + Integer.toString(sdrBandNo[i]),
                                                ProductData.TYPE_INT16);
            sdrOutputBand.setDescription(
                    "Surface directional reflectance at " + band.getSpectralWavelength() + " nm");
            sdrOutputBand.setUnit("1");
            sdrOutputBand.setScalingFactor(SCALING_FACTOR);
            ProductUtils.copySpectralBandProperties(band, sdrOutputBand);
            sdrOutputBand.setNoDataValueUsed(true);
            sdrOutputBand.setGeophysicalNoDataValue(-1);
            sdrBands[i] = sdrOutputBand;
        }
        // create and add the NDVI flags coding
        FlagCoding sdiFlagCoding = createSdiFlagCoding(targetProduct);
        targetProduct.getFlagCodingGroup().add(sdiFlagCoding);

        // create and add the SDR flags dataset
        sdrFlagBand = targetProduct.addBand(SDR_FLAGS_BAND_NAME,
                               ProductData.TYPE_UINT16);
        sdrFlagBand.setDescription("SDR specific flags");
        sdrFlagBand.setSampleCoding(sdiFlagCoding);

        validBand = maskProduct.getBand(validBandName);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {

        pm.beginTask("Processing frame...", rectangle.height);
        try {
            Tile sza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), rectangle);
            Tile saa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME), rectangle);
            Tile vza = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), rectangle);
            Tile vaa = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME), rectangle);

            Tile ang = null;
            if (StringUtils.isNotNullAndNotEmpty(angName)) {
            	ang = getSourceTile(aerosolProduct.getBand(angName), rectangle);
            }
            Tile aot470 = getSourceTile(aerosolProduct.getBand(aot470Name), rectangle);


            Tile[] reflectance = new Tile[sdrBandNo.length];
            Tile[] sdr = new Tile[sdrBandNo.length];
            
            for (int i = 0; i < sdrBandNo.length; i++) {
                reflectance[i] = getSourceTile(reflectanceBands[i], rectangle);
            }
            Tile isValidPixel = getSourceTile(validBand, rectangle);

            for (int i = 0; i < sdrBands.length; i++) {
            	sdr[i] = targetTiles.get(sdrBands[i]);
            }
            Tile sdrFlag = targetTiles.get(sdrFlagBand);

            SdrAlgorithm clonedAlgorithm = algorithm.clone();

            final double[] sdrAlgoInput = new double[9];
            final double[] sdrAlgoOutput = new double[1];
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
				for (int x = rectangle.x; x < rectangle.x+rectangle.width; x++) {
	                if (isValidPixel.getSampleBoolean(x, y)) {
	                    double t_sza = sza.getSampleDouble(x, y) * MathUtils.DTOR;
	                    double t_vza = vza.getSampleDouble(x, y) * MathUtils.DTOR;
	                    double ada = AlbedoUtils.computeAzimuthDifference(vaa.getSampleDouble(x, y), saa.getSampleDouble(x, y)) * MathUtils.DTOR;
                        double mueSun = Math.cos(t_sza);
	                    double geomX = Math.sin(t_vza) * Math.cos(ada);
	                    double geomY = Math.sin(t_vza) * Math.sin(ada);
	                    double geomZ = Math.cos(t_vza);

	                    double t_sdr;
	                    short sdrFlags = 0;
	                    for (int bandId = 0; bandId < reflectanceBands.length; bandId++) {
	                        final Band reflInputBand = reflectanceBands[bandId];
                            double rhoNorm = reflectance[bandId].getSampleDouble(x, y) / Math.PI;
                            double wavelength = reflInputBand.getSpectralWavelength();
                            sdrAlgoInput[0] = rhoNorm;
	                        sdrAlgoInput[1] = wavelength;
	                        sdrAlgoInput[2] = mueSun;
	                        sdrAlgoInput[3] = geomX;
	                        sdrAlgoInput[4] = geomY;
	                        sdrAlgoInput[5] = geomZ;
	                        sdrAlgoInput[6] = aot470.getSampleDouble(x, y);
	                        sdrAlgoInput[7] = 0; // aot 660; usage discontinued
	                        if (ang != null) {
	                        	sdrAlgoInput[8] = ang.getSampleDouble(x, y);
	                        } else {
	                        	sdrAlgoInput[8] = angValue;
	                        }
                            clonedAlgorithm.computeSdr(sdrAlgoInput, sdrAlgoOutput);
	                        t_sdr = sdrAlgoOutput[0];
	                        if (Double.isInfinite(t_sdr) || Double.isNaN(t_sdr)) {
	                            t_sdr = 0.0;
	                            sdrFlags |= 1 << (reflInputBand.getSpectralBandIndex() + 1);
	                        } else if (t_sdr < 0.0) {
	                            t_sdr = 0.0;
	                            sdrFlags |= 1 << (reflInputBand.getSpectralBandIndex() + 1);
	                        } else if (t_sdr > 1.0) {
	                            t_sdr = 1.0;
	                            sdrFlags |= 1 << (reflInputBand.getSpectralBandIndex() + 1);
	                        }
	                        sdr[bandId].setSample(x, y, (float) t_sdr);
	                    }
	                    // Combine SDR-Flags to single INVALID Flag
	                    sdrFlags |= (sdrFlags == 0 ? 0 : 1); 
	                    sdrFlag.setSample(x, y, sdrFlags);
	                } else {
	                    for (int j = 0; j < reflectanceBands.length; j++) {
	                        sdr[j].setSample(x, y, -1);
	                    }
	                    sdrFlag.setSample(x, y, SDR_INVALID_FLAG_VALUE);
	                }
				}
			}
			pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private void loadNeuralNet() throws IOException, JnnException {
        // OLD Beam:
//        String auxdataSrcPath = "auxdata/sdr";
//        final String auxdataDestPath = ".beam/" +
//                AlbedomapConstants.SYMBOLIC_NAME + "/" + auxdataSrcPath;
//        File auxdataTargetDir = new File(SystemUtils.getUserHomeDir(), auxdataDestPath);
//        URL sourceUrl = ResourceInstaller.getSourceUrl(this.getClass());
//
//        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceUrl, auxdataSrcPath, auxdataTargetDir);
//        resourceInstaller.install(".*", new NullProgressMonitor());

        // todo: clarify if this is the right way in Snap/S3tbx:
        final Path auxdataDirPath = SystemUtils.getAuxDataPath().resolve("ctp").toAbsolutePath();
        File auxdataTargetDir = auxdataDirPath.toFile();
        Path sourcePath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata");
        try {
            new ResourceInstaller(sourcePath, auxdataDirPath).install(".*", ProgressMonitor.NULL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        File nnFile = new File(auxdataTargetDir, neuralNetFile);
        final JnnNet neuralNet;
        try (InputStreamReader reader = new FileReader(nnFile)) {
            Jnn.setOptimizing(true);
            neuralNet = Jnn.readNna(reader);
        }
        algorithm = new SdrAlgorithm(neuralNet);
    }


    private FlagCoding createSdiFlagCoding(Product outputProduct) {
        final double rf1 = 0.3;
        final double gf1 = 1.0;
        final double bf1 = 0.5;

        final FlagCoding flagCoding = new FlagCoding(SDR_FLAGS_BAND_NAME);
        flagCoding.setDescription("SDR Flag Coding");

        final MetadataAttribute invAttr = new MetadataAttribute(SDR_INVALID_FLAG_NAME, ProductData.TYPE_INT32);
        invAttr.getData().setElemInt(SDR_INVALID_FLAG_VALUE);
        invAttr.setDescription("SDR spectrum is invalid");
        flagCoding.addAttribute(invAttr);

        int index = 0;
        int w = outputProduct.getSceneRasterWidth();
        int h = outputProduct.getSceneRasterHeight();
        Mask mask;

        double a = 2 * Math.PI * (0 / 31.0);
        Color color = new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                                (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                                (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
        mask = Mask.BandMathsType.create(invAttr.getName(),
                                         invAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + invAttr.getName(),
                                         color, 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);

        for (int i = 0; i < sdrBands.length; i++) {
            final Band sdrBand = sdrBands[i];
            final String flagName = "INVALID_" + sdrBand.getName().toUpperCase();
            final String flagDesc = "Invalid " + sdrBand.getDescription();

            flagCoding.addFlag(flagName,
                               1 << (sdrBand.getSpectralBandIndex() + 1),
                               flagDesc);

            a = 2 * Math.PI * (i + 1 / 31.0);
            color = new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                              (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                              (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
            mask = Mask.BandMathsType.create(flagName,
                                             flagDesc, w, h,
                                             flagCoding.getName() + "." + flagName,
                                             color, 0.4f);
            outputProduct.getMaskGroup().add(index++, mask);
        }
        return flagCoding;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SdrOp.class);
        }
    }
}