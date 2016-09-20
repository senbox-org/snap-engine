package org.esa.s3tbx.fub.wew;


import org.esa.s3tbx.fub.wew.util.NN_AtmCorr;
import org.esa.s3tbx.fub.wew.util.NN_CHL;
import org.esa.s3tbx.fub.wew.util.NN_TSM;
import org.esa.s3tbx.fub.wew.util.NN_YellowSubstance;
import org.esa.s3tbx.fub.wew.util.WaterProcessorOzone;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.stream.Stream;

@OperatorMetadata(alias = "FUB.Water", authors = "Thomas Schroeder, Michael Schaale",
        copyright = "Institute for Space Sciences (WeW), Freie Universitaet Berlin",
        category = "Optical/Thematic Water Processing",
        version = "4.0.1",
        description = "FUB/WeW WATER Processor to retrieve case II water properties and atmospheric properties")
public class WaterProcessorOp extends PixelOperator {

    private float[] solarFlux;

    private Band[] inputBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
    private Raster validMaskData;

    @SourceProduct(label = "Source product",
            description = "The MERIS L1b or L1P source product used for the processing.")
    private Product sourceProduct;

    @Parameter(description = "Whether chlorophyll-a concentration band shall be computed", defaultValue = "true",
            label = "Compute chlorophyll-a concentration band")
    private boolean computeCHL;

    @Parameter(description = "Whether yellow substances band shall be computed", defaultValue = "true",
            label = "Compute yellow substances band")
    private boolean computeYS;

    @Parameter(description = "Whether total suspended matter band shall be computed", defaultValue = "true",
            label = "Compute total suspended matter band")
    private boolean computeTSM;

    @Parameter(description = "Whether atmospheric correction bands shall be computed", defaultValue = "true",
            label = "Compute water leaving reflectances and AOT bands")
    private boolean computeAtmCorr;

    @Parameter(description = "Expert parameter. Performs a check whether the '" + WaterProcessorOpConstant.SUSPECT_FLAG_NAME + "' shall be considered in an expression." +
            "This parameter is only considered when the expression contains the term '" + WaterProcessorOpConstant.SUSPECT_EXPRESSION_TERM + "'",
            defaultValue = "true", label = "Check whether '" + WaterProcessorOpConstant.SUSPECT_FLAG_NAME + "' is valid")
    private boolean checkWhetherSuspectIsValid;

    // TODO (mp/20160704) -  For OLCI: !quality_flags.invalid && (!quality_flags.land || quality_flags.fresh_inland_water)
    @Parameter(description = "Band maths expression which defines valid pixels. If the expression is empty," +
            "all pixels will be considered.",
            defaultValue = "not l1_flags.GLINT_RISK and not l1_flags.BRIGHT and not l1_flags.INVALID " + WaterProcessorOpConstant.SUSPECT_EXPRESSION_TERM,
            label = "Use valid pixel expression")
    private String expression;
    private Sensor sensor;

    private static FlagCoding createResultFlagCoding() {
        FlagCoding resultFlagCoding = new FlagCoding(WaterProcessorOpConstant.result_flags_name);
        resultFlagCoding.setDescription("RESULT Flag Coding");
        for (int i = 0; i < WaterProcessorOpConstant.RESULT_ERROR_NAMES.length; i++) {
            MetadataAttribute attribute = new MetadataAttribute(WaterProcessorOpConstant.RESULT_ERROR_NAMES[i], ProductData.TYPE_INT32);
            attribute.getData().setElemInt(WaterProcessorOpConstant.RESULT_ERROR_VALUES[i]);
            attribute.setDescription(WaterProcessorOpConstant.RESULT_ERROR_TEXTS[i]);
            resultFlagCoding.addAttribute(attribute);
        }
        return resultFlagCoding;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        sensor = getSensorPattern();
        String[] sourceRasterNames = new String[0];

        if (sensor == Sensor.MERIS) {
            sourceRasterNames = WaterProcessorOpConstant.SOURCE_RASTER_NAMES_MERIS;
        } else if (sensor == Sensor.OLCI) {
            sourceRasterNames = WaterProcessorOpConstant.SOURCE_RASTER_NAMES_OLCI;
        }

        for (int i = 0; i < inputBands.length; i++) {
            String radianceBandName = sourceRasterNames[i];
            Band radianceBand = sourceProduct.getBand(radianceBandName);
            if (radianceBand == null) {
                throw new OperatorException(String.format("Missing input band '%s'.", radianceBandName));
            }
            if (radianceBand.getSpectralWavelength() <= 0.0) {
                throw new OperatorException(String.format("Input band '%s' does not have wavelength information.", radianceBandName));
            }
            inputBands[i] = radianceBand;
        }

        solarFlux = getSolarFlux(sourceProduct, inputBands);
        if (checkWhetherSuspectIsValid) {
            checkWhetherSuspectIsValid();
        }
        final PlanarImage validMaskImage = createValidMaskImage(sourceProduct, expression);
        validMaskData = validMaskImage.getData();
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        String[] sourceRasterNames = new String[0];
        if (sensor == Sensor.MERIS) {
            sourceRasterNames = WaterProcessorOpConstant.SOURCE_RASTER_NAMES_MERIS;
        } else if (sensor == Sensor.OLCI) {
            sourceRasterNames = WaterProcessorOpConstant.SOURCE_RASTER_NAMES_OLCI;
        }
        for (int i = 0; i < sourceRasterNames.length; i++) {
            sampleConfigurer.defineSample(i, sourceRasterNames[i]);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        String[] bandNames = new String[0];
        if (computeCHL) {
            bandNames = StringUtils.addToArray(bandNames, WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_NAMES[0]);
        }
        if (computeYS) {
            bandNames = StringUtils.addToArray(bandNames, WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_NAMES[1]);
        }
        if (computeTSM) {
            bandNames = StringUtils.addToArray(bandNames, WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_NAMES[2]);
        }
        if (computeAtmCorr) {
            bandNames = StringUtils.addArrays(bandNames, WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_NAMES);
            bandNames = StringUtils.addArrays(bandNames, WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_NAMES);
        }
        bandNames = StringUtils.addToArray(bandNames, WaterProcessorOpConstant.result_flags_name);

        for (int i = 0; i < bandNames.length; i++) {
            sampleConfigurer.defineSample(i, bandNames[i]);
        }
    }

    @Override
    protected void computePixel(final int xpos, final int ypos, Sample[] sourceSamples, WritableSample[] targetSamples) {

        // If set to -1.0f : NN input and output ranges are checked
        // If set to +1.0f : NN input and output ranges are NOT checked
        float aset = -1.0f;

        int width = 1;
        int nbands = inputBands.length;

        // allocate memory for a multispectral scan line
        float[] toa = new float[nbands];

        // allocate memory for the flags
        int[] resultFlags = new int[width];

        // Ozone data
        double[] wavelength = new double[nbands];
        double[] exO3 = new double[nbands];

        // local variables
        double d2r;
        float dazi;
        int k;
        int l;
        int ls = 0;
        int n;
        float[] a = new float[width];

        int inodes;
        int onodes;
        int onodes_1;
        int onodes_2;
        int stage;

        float[][] ipixel = new float[2][1];
        float[][] ipixels = new float[2][1];
        float[][] opixel = new float[2][1];

        // Load the wavelengths and ozone spectral extinction coefficients
        //
        for (int i = 0; i < nbands; i++) {
            wavelength[i] = inputBands[i].getSpectralWavelength();
            exO3[i] = WaterProcessorOzone.O3excoeff(wavelength[i]);
        }

        d2r = Math.acos(-1.0) / 180.0;

        // Get the number of I/O nodes in advance
        inodes = NN_YellowSubstance.compute(ipixel, -1, opixel, 1, width, resultFlags, 0, a);
        // implicit atm.corr.
        onodes_1 = NN_YellowSubstance.compute(ipixel, 1, opixel, -1, width, resultFlags, 0, a);
        // explicit atm.corr.
        onodes_2 = NN_AtmCorr.compute(ipixel, 1, opixel, -1, width, resultFlags, 0, a);

        int num_toa = 12;
        int output_planes = 0;
        if (computeCHL) {
            output_planes++;
        }
        if (computeYS) {
            output_planes++;
        }
        if (computeTSM) {
            output_planes++;
        }
        if (computeAtmCorr) {
            output_planes += WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_NAMES.length + WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_NAMES.length;
        }
        float[] top = new float[num_toa];
        float[] tops = new float[num_toa];
        double[] o3f = new double[num_toa];
        float[] sof = new float[num_toa];
        float[] aux = new float[2];
        float[] geo = new float[4];
        float[] result = new float[output_planes];

        // First the TOA radiances
        for (n = 0; n < inputBands.length; n++) {
            toa[n] = sourceSamples[n].getFloat();
        } // n

        // Second the auxiliary data
        float sza = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_SUN_ZENITH].getFloat();
        float saa = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_SUN_AZIMUTH].getFloat();
        float vza = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_VIEW_ZENITH].getFloat();
        float vaa = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_VIEW_AZIMUTH].getFloat();
        float zw = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_ZONAL_WIND].getFloat();
        float mw = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_MERID_WIND].getFloat();
        float press = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_ATM_PRESS].getFloat();
        float o3 = sourceSamples[WaterProcessorOpConstant.SOURCE_SAMPLE_INDEX_OZONE].getFloat();

        final int x = 0;

        resultFlags[x] = 0;
        int resultFlagsNN = 0;

        // Exclude pixels from processing if the following l1flags mask becomes true
//        not quality_flags_sun_glint_risk and not quality_flags_bright and not quality_flags_invalid
//
        if (validMaskData.getSample(xpos, ypos, 0) == 0) {
            resultFlags[x] = WaterProcessorOpConstant.RESULT_ERROR_VALUES[0];
        }

        // *********************
        // * STAGE 0
        // *********************

        // Get the toa reflectances for selected bands
        // and normalize ozone
        //
        l = 0;
        final double TOTAL_OZONE_DU_MOMO = 344.0;
        for (n = 0; n <= 6; n++, l++) {
            tops[l] = toa[n];
            sof[l] = solarFlux[n];
            top[l] = toa[n] / solarFlux[n];
            o3f[l] = Math.exp(-(TOTAL_OZONE_DU_MOMO - o3) * exO3[n] / 1000.0 * (1.0 / Math.cos(
                    (double) vza * d2r) + 1.0 / Math.cos((double) sza * d2r)));
            top[l] *= o3f[l];
        }
        for (n = 8; n <= 9; n++, l++) {
            tops[l] = toa[n];
            sof[l] = solarFlux[n];
            top[l] = toa[n] / solarFlux[n];
            o3f[l] = Math.exp(-(TOTAL_OZONE_DU_MOMO - o3) * exO3[n] / 1000.0 * (1.0 / Math.cos(
                    (double) vza * d2r) + 1.0 / Math.cos((double) sza * d2r)));
            top[l] *= o3f[l];
        }
        for (n = 11; n <= 13; n++, l++) {
            tops[l] = toa[n];
            sof[l] = solarFlux[n];
            top[l] = toa[n] / solarFlux[n];
            o3f[l] = Math.exp(-(TOTAL_OZONE_DU_MOMO - o3) * exO3[n] / 1000.0 * (1.0 / Math.cos(
                    (double) vza * d2r) + 1.0 / Math.cos((double) sza * d2r)));
            top[l] *= o3f[l];
        }

        // Get the wind speed
        aux[0] = (float) Math.sqrt((double) (zw * zw + mw * mw));
        // Get the pressure
        aux[1] = press;

        // Adjust the azimuth difference
        dazi = vaa - saa;

        while (dazi <= -180.0f) {
            dazi += 360.0f;
        }
        while (dazi > 180.0f) {
            dazi -= 360.0f;
        }
        float tmp = dazi;
        if (tmp >= 0.0f) {
            dazi = +180.0f - dazi;
        }
        if (tmp < 0.0f) {
            dazi = -180.0f - dazi;
        }

        // Get cos(sunzen)
        geo[0] = (float) Math.cos((double) sza * d2r);

        // And now transform into cartesian coordinates
        geo[1] = (float) (Math.sin((double) vza * d2r) * Math.cos((double) dazi * d2r)); // obs_x
        geo[2] = (float) (Math.sin((double) vza * d2r) * Math.sin((double) dazi * d2r)); // obs_y
        geo[3] = (float) (Math.cos((double) vza * d2r));                            // obs_z

        // *********************
        // * STAGE 1-4
        // *********************

        onodes = onodes_1; // They differ !!

        ipixel = new float[inodes][width];
        ipixels = new float[inodes][width];
        opixel = new float[onodes][width];

        // load the TOA reflectances
        for (l = 0; l < num_toa; l++) {
            ipixel[l][x] = top[l];
        }

        // get the wind speed and pressure
        ipixel[l++][x] = aux[0];
        ipixel[l++][x] = aux[1];

        // get cos(sunzen), x, yPos, z
        ipixel[l++][x] = geo[0];
        ipixel[l++][x] = geo[1];
        ipixel[l++][x] = geo[2];
        ipixel[l++][x] = geo[3];

        // Check against range limits inside the network
        // recall if the value of a[x] is set to -1.0f.
        //
        // This results in the application of the flag
        // 'RESULT_ERROR_VALUE[]' to the 'resultFlagsNN'


        a[x] = aset;

        // Save input pixel
        ls = l;
        for (l = 0; l < ls; l++) {
            ipixels[l][x] = ipixel[l][x];
        }

        int resultCounter = 0;

        if (computeCHL) {
            // Run the 1-step chlorophyll network;
            stage = 1;
            NN_CHL.compute(ipixel, inodes, opixel, onodes, width, resultFlags, 0, a);

            // Input range failure
            if ((a[x] > -2.1) && (a[x] < -1.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
            }
            // Output range failure
            if ((a[x] > -19.1) && (a[x] < -18.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            // Input AND Output range failure
            if ((a[x] > -22.1) && (a[x] < -21.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            result[resultCounter++] = opixel[0][x];
        }
        if (computeYS) {
            // Run the 1-step yellow substance network;
            stage = 2;
            // reload the pixel
            for (l = 0; l < ls; l++) {
                ipixel[l][x] = ipixels[l][x];
            }
            a[x] = aset;

            NN_YellowSubstance.compute(ipixel, inodes, opixel, onodes, width, resultFlags, 0, a);

            // Input range failure
            if ((a[x] > -2.1) && (a[x] < -1.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
            }
            // Output range failure
            if ((a[x] > -19.1) && (a[x] < -18.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            // Input AND Output range failure
            if ((a[x] > -22.1) && (a[x] < -21.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            result[resultCounter++] = opixel[0][x];
        }
        if (computeTSM) {
            // Run the 1-step total suspended matter network;
            stage = 3;

            // reload the pixel
            for (l = 0; l < ls; l++) {
                ipixel[l][x] = ipixels[l][x];
            }
            a[x] = aset;

            NN_TSM.compute(ipixel, inodes, opixel, onodes, width, resultFlags, 0, a);

            // Input range failure
            if ((a[x] > -2.1) && (a[x] < -1.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
            }
            // Output range failure
            if ((a[x] > -19.1) && (a[x] < -18.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            // Input AND Output range failure
            if ((a[x] > -22.1) && (a[x] < -21.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            result[resultCounter++] = opixel[0][x];
        }
        if (computeAtmCorr) {
            // Run part 1 of the 2-step atm.corr. network;
            stage = 4;
            onodes = onodes_2;
            opixel = new float[onodes][width];

            // reload the pixel
            for (l = 0; l < ls; l++) {
                ipixel[l][x] = ipixels[l][x];
            }
            a[x] = aset;

            NN_AtmCorr.compute(ipixel, inodes, opixel, onodes, width, resultFlags, 0, a);

            // Input range failure
            if ((a[x] > -2.1) && (a[x] < -1.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
            }
            // Output range failure
            if ((a[x] > -19.1) && (a[x] < -18.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            // Input AND Output range failure
            if ((a[x] > -22.1) && (a[x] < -21.9)) {
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage - 1];
                resultFlagsNN |= WaterProcessorOpConstant.RESULT_ERROR_VALUES[2 * stage];
            }
            //System.out.println(" --> " + resultFlagsNN[x]);

            // The aots
            final int num_msl = 8;
            for (int i = num_msl; i < onodes; i++) {
                result[resultCounter + i - num_msl] = opixel[i][x];
            }
            for (int i = 0; i < num_msl; i++) {
                final int numOfSpectralAerosolOpticalDepths = 4;
                result[resultCounter + numOfSpectralAerosolOpticalDepths + i] = opixel[i][x];
            }
        }
        // Now check for error flags !
        // If set, set output vector to mask value !
        if (resultFlags[x] != 0) {
            for (n = 0; n < output_planes; n++) {
                result[n] = WaterProcessorOpConstant.RESULT_MASK_VALUE;
            }
        }
        // Check for angle out of range. NNs only trained for MERIS. The vza for OLCI is higher.
        if (vza >= 40) {
            resultFlags[x] = WaterProcessorOpConstant.RESULT_ERROR_VALUES[0];
        }

        // Combine result flags
        resultFlags[x] |= resultFlagsNN;

        // Set sample values in target product
        //
        for (n = 0; n < output_planes; n++) {
            targetSamples[n].set(result[n]);
        }
        targetSamples[output_planes].set(resultFlags[0]);
    }

    private void checkWhetherSuspectIsValid() {
        if (!expression.contains(WaterProcessorOpConstant.SUSPECT_EXPRESSION_TERM)) {
            return;
        }
        final int height = sourceProduct.getSceneRasterHeight();
        final int width = sourceProduct.getSceneRasterWidth();

        int k;// Some Level 1b scenes mark almost all pixels as 'suspect'. This is obviously nonsense.
        // Because we would like to make use of the suspect flag in mask mask_to_be_used we do
        // check first if it behaves fine, ie the number of suspect pixels for one line in the
        // middle of the scene should be below 50 % . Else we do not make use of the suspect flag
        // in the mask mask_to_be_used.

        // Grab a line in the middle of the scene
        final Band l1FlagsInputBand = sourceProduct.getBand(sensor.getFlagName());
        int[] l1Flags = new int[width];
        final int halfHeight = height / 2;
        try {
            l1FlagsInputBand.readPixels(0, halfHeight, width, 1, l1Flags);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // The input type pattern for ICOL products
        final String ICOL_PATTERN = "MER_.*1N";
        boolean icolMode = sourceProduct.getProductType().matches(ICOL_PATTERN);
        if (icolMode) {
            expression = expression.replace(WaterProcessorOpConstant.SUSPECT_EXPRESSION_TERM, "");
            System.out.println("--- Input product is of type icol ---");
            System.out.println("--- Switching to relaxed mask. ---");
        } else {
            final PlanarImage validMaskImage = createValidMaskImage(sourceProduct, sensor.getSuspectFlag());
            final Raster validData = validMaskImage.getData();
            k = 0;
            // Now sum up the cases which signal a suspect behaviour
            for (int i = 0; i < width; i++) {
                if (validData.getSample(i, halfHeight, 0) != 0) {
                    k++;
                }
            }
            // more than 50 percent ?
            if (k >= width / 2) {
                // Do not make use of the suspect flag
                expression = expression.replace(WaterProcessorOpConstant.SUSPECT_EXPRESSION_TERM, "");
//                maskToBeUsed = mask_to_be_used;
                final float percent = (float) k / (float) width * 100.0f;
                System.out.println("--- " + percent + " % of the scan line are marked as suspect ---");
                System.out.println("--- Switching to relaxed mask. ---");
            }
        }
    }

    /*
    * Reads the solar spectral fluxes for all MERIS L1b bands.
    *
    * Sometimes the file do not contain solar fluxes. As they do
    * show heavy variations over the year or for slight wavelength
    * shifts we do use some defaults if necessary.
    */
    private float[] getSolarFlux(Product product, Band[] bands) {
        float[] dsf = getSolarFluxFromMetadata(product);
        if (dsf == null) {
            dsf = new float[bands.length];
            final double[] defsol = new double[]{
                    1670.5964, 1824.1444, 1874.9883,
                    1877.6682, 1754.7749, 1606.6401,
                    1490.0026, 1431.8726, 1369.2035,
                    1231.7164, 1220.0767, 1144.9675,
                    932.3497, 904.8193, 871.0908
            };
            for (int i = 0; i < bands.length; i++) {
                Band band = bands[i];
                dsf[i] = band.getSolarFlux();
                if (dsf[i] <= 0.0) {
                    dsf[i] = (float) defsol[i];
                }
            }
        }
        return dsf;
    }

    private float[] getSolarFluxFromMetadata(Product product) {
        MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataElement gadsElem = metadataRoot.getElement("Scaling_Factor_GADS");
        if (gadsElem != null) {
            MetadataAttribute solarFluxAtt = gadsElem.getAttribute("sun_spec_flux");
            if (solarFluxAtt != null) {
                return (float[]) solarFluxAtt.getDataElems();
            }
        }
        return null;
    }

    private Sensor getSensorPattern() {
        String[] bandNames = getSourceProduct().getBandNames();
        boolean isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("Oa\\d+_radiance"));
        if (isSensor) {
            return Sensor.OLCI;
        }
        isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("radiance_\\d+"));

        if (isSensor) {
            return Sensor.MERIS;
        }
        throw new OperatorException("The operator can't be applied on the sensor");
    }


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        final Product sourceProduct = productConfigurer.getSourceProduct();
        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.setProductType(getOutputProductType());

        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();

        if (computeCHL) {
            addConcentrationBand(targetProduct, sceneWidth, sceneHeight, 0);
        }
        if (computeYS) {
            addConcentrationBand(targetProduct, sceneWidth, sceneHeight, 1);
        }
        if (computeTSM) {
            addConcentrationBand(targetProduct, sceneWidth, sceneHeight, 2);
        }
        if (computeAtmCorr) {
            addOpticalDepthBands(targetProduct, sceneWidth, sceneHeight);
            addReflectanceBands(targetProduct, sceneWidth, sceneHeight);
        }

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        if (!targetProduct.containsBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME)) {
            productConfigurer.copyBands(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME);
        }
        if (!targetProduct.containsBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME)) {
            productConfigurer.copyBands(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME);
        }
        productConfigurer.copyBands(EnvisatConstants.MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME);

        FlagCoding resultFlagCoding = createResultFlagCoding();
        targetProduct.getFlagCodingGroup().add(resultFlagCoding);

        final Band resultFlagsOutputBand = targetProduct.addBand(WaterProcessorOpConstant.result_flags_name, ProductData.TYPE_UINT16);
        resultFlagsOutputBand.setDescription("FUB/WeW WATER plugin specific flags");
        resultFlagsOutputBand.setSampleCoding(resultFlagCoding);

        productConfigurer.copyMasks();

        String flagNamePrefix = WaterProcessorOpConstant.result_flags_name + ".";
        addMasksToTargetProduct(targetProduct, sceneWidth, sceneHeight, flagNamePrefix);
    }

    private void addMasksToTargetProduct(Product targetProduct, int sceneWidth, int sceneHeight, String flagNamePrefix) {
        ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
        Color[] colors = new Color[]{
                Color.cyan, Color.green, Color.green, Color.yellow, Color.yellow,
                Color.orange, Color.orange, Color.blue, Color.blue
        };
        for (int i = 0; i < WaterProcessorOpConstant.RESULT_ERROR_NAMES.length; i++) {
            maskGroup.add(Mask.BandMathsType.create(WaterProcessorOpConstant.RESULT_ERROR_NAMES[i].toLowerCase(), WaterProcessorOpConstant.RESULT_ERROR_TEXTS[i],
                    sceneWidth, sceneHeight, flagNamePrefix + WaterProcessorOpConstant.RESULT_ERROR_NAMES[i],
                    colors[i], WaterProcessorOpConstant.RESULT_ERROR_TRANSPARENCIES[i]));

        }
    }

    private void addReflectanceBands(Product targetProduct, int sceneWidth, int sceneHeight) {
        for (int i = 0; i < WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_NAMES.length; i++) {
            final Band band = createBand(WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_NAMES[i], sceneWidth, sceneHeight);
            band.setDescription(WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_DESCRIPTIONS[i]);
            band.setUnit(WaterProcessorOpConstant.OUTPUT_REFLECTANCE_BAND_UNITS[i]);
            band.setSpectralWavelength(WaterProcessorOpConstant.RHO_W_LAMBDA[i]);
            band.setSpectralBandwidth(WaterProcessorOpConstant.RHO_W_BANDW[i]);
            band.setSpectralBandIndex(i);
            band.setNoDataValue(WaterProcessorOpConstant.RESULT_MASK_VALUE);
            band.setNoDataValueUsed(true);
            targetProduct.addBand(band);
        }
    }

    private void addOpticalDepthBands(Product targetProduct, int sceneWidth, int sceneHeight) {
        for (int i = 0; i < WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_NAMES.length; i++) {
            final Band band = createBand(WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_NAMES[i], sceneWidth, sceneHeight);
            band.setDescription(WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_DESCRIPTIONS[i]);
            band.setUnit(WaterProcessorOpConstant.OUTPUT_OPTICAL_DEPTH_BAND_UNITS[i]);
            band.setSpectralWavelength(WaterProcessorOpConstant.TAU_LAMBDA[i]);
            band.setSpectralBandIndex(i);
            band.setNoDataValue(WaterProcessorOpConstant.RESULT_MASK_VALUE);
            band.setNoDataValueUsed(true);
            targetProduct.addBand(band);
        }
    }

    private void addConcentrationBand(Product targetProduct, int sceneWidth, int sceneHeight, int concentrationBandIndex) {
        final Band band = createBand(WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_NAMES[concentrationBandIndex], sceneWidth, sceneHeight);
        band.setDescription(WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_DESCRIPTIONS[concentrationBandIndex]);
        band.setUnit(WaterProcessorOpConstant.OUTPUT_CONCENTRATION_BAND_UNITS[concentrationBandIndex]);
        band.setNoDataValueUsed(true);
        band.setNoDataValue(5.0);
        targetProduct.addBand(band);
    }

    private Band createBand(String bandName, int sceneWidth, int sceneHeight) {
        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        band.setScalingOffset(0.0);
        band.setScalingFactor(1.0);
        band.setSpectralBandIndex(0);
        return band;
    }

    private String getOutputProductType() throws OperatorException {
        String sourceType = sourceProduct.getProductType();
        if (sourceType != null) {
            return String.format("%s_FLH_MCI", sourceType);
        } else {
            return "FLH_MCI";
        }
    }

    private PlanarImage createValidMaskImage(Product product, String expression) {
        if (StringUtils.isNullOrEmpty(expression)) {
            return createEmptyMask(product);
        }

        if (product.isCompatibleBandArithmeticExpression(expression)) {
            return VirtualBandOpImage.builder(expression, product)
                    .dataType(ProductData.TYPE_UINT8)
                    .fillValue(0)
                    .create();
        } else {
            String msg = String.format("Parameter 'expression' is not compatible with the source product. Expression is '%s'", expression);
            throw new OperatorException(msg);
        }
    }


    private PlanarImage createEmptyMask(Product product) {
        return ConstantDescriptor.create((float) product.getSceneRasterWidth(),
                (float) product.getSceneRasterHeight(),
                new Byte[]{-1}, null);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WaterProcessorOp.class);
        }
    }

    private enum Sensor {
        MERIS("l1_flags", "l1_flags.SUSPECT"),
        OLCI("quality_flags", "quality_flags.dubious");

        private final String suspectFlag;

        public String getFlagName() {
            return flagName;
        }

        private final String flagName;

        public String getSuspectFlag() {
            return suspectFlag;
        }

        Sensor(String flagName, String suspectFalg) {
            this.flagName = flagName;
            this.suspectFlag = suspectFalg;
        }
    }
}
