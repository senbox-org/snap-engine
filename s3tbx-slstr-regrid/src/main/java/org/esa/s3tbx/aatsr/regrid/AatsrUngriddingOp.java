package org.esa.s3tbx.aatsr.regrid;

import org.esa.snap.core.datamodel.Band;
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
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Alasdhair Beaton (Telespazio VEGA)
 * @author Philip Beavis (Telespazio VEGA)
 */
@OperatorMetadata(description = "Ungrids (A)ATSR L1B products and extracts geolocation and pixel field of view data.",
        alias = "AATSR.Ungrid", authors = "Alasdhair Beaton, Philip Beavis", version = "1.0",
        category = "Optical/Geometric", label = "AATSR Ungridding", copyright = "(c) 2016 by Telespazio VEGA UK Ltd."
)
public class AatsrUngriddingOp extends PixelOperator {

    @SourceProduct(description = "(A)ATSR-1/2 source product (Envisat *.N1 format)")
    Product sourceProduct;

    @TargetProduct(description = "e.g. hdf5 or netcdf4 cf")
    Product targetProduct;

    // notNull does not work - test is applied in dialog after processing has occurred?
    @Parameter(notNull = false, description = "L1B characterisation file is needed to specify first forward pixel and first nadir pixel")
    private File L1BCharacterisationFile;

    @Parameter(label = "Use pixel corner as reference", defaultValue = "true", description = "Choose the pixel coordinate reference point for use in the output file. \nCheck for Corner (default), un-check for Centre.")
    private boolean cornerReferenceFlag = true;

    @Parameter(label = "Topographic corrections to tie points", defaultValue = "false", description = "Option to apply topographic corrections to tie points")
    private boolean topographicFlag = false;

    @Parameter(defaultValue = "0.05", description = "Distance (image coordinates) pixel can be from tie-point to have topo correction applied")
    private double topographyHomogenity = 0.05;

    //@Parameter (label = "Include FOV (Experimental!)", defaultValue = "false", description = "Adds four bands to indicate field of view.\nAn FOV file is required only if this is checked.")
    private boolean enableFOV = false;

    //@Parameter ( description = "AATSR FOV calibration measurement file")
    private File FOVMeasurementFile;

    //@Parameter (label = "Extent of IFOV to report", defaultValue = "0.4", description = "Extent of IFOV to report as distance in pixel projection")
    private double pixelIFOVReportingExtent = 0.4;

    private InputParameters parameters;

    // Annotation Data Sets from product
    private ProductNodeGroup<MetadataElement> NADIR_VIEW_SCAN_PIX_NUM_ADS_Records;
    private ProductNodeGroup<MetadataElement> FWARD_VIEW_SCAN_PIX_NUM_ADS_Records;
    private ProductNodeGroup<MetadataElement> SCAN_PIXEL_X_AND_Y_ADS_Records;
    private ProductNodeGroup<MetadataElement> GEOLOCATION_ADS_Records;

    private List<Double> scanYCoords;
    private int s0;    // s0 scan number of first record = 32
    private List<List<Double>> pixelProjectionMap;

    private final static int PIXELS_PER_ROW = 512;
    //default values as given in the technical note
    private final static int DEFAULT_FIRST_FORWARD_PIXEL = 1305;
    private final static int DEFAULT_FIRST_NADIR_PIXEL = 213;

    // PointOperator::initialise() step 1
    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        //check source product type
        if (!sourceProduct.getProductType().equals("ATS_TOA_1P")) {
            throw new OperatorException("Product does not have correct type");
        }
        //System.out.println(sourceProduct.getBandNames());

        // What does this do?
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");

        prepareInputParameters();
        //prepareSubset();
        prepareMetadata();

        if (enableFOV) {
            // Get the pixel projection map (along and across track extent) for all 2000 pixels
            // This assumes spherical earth geometry & constant platform altitude
            this.pixelProjectionMap = new ArrayList<>();
            Calculator.getConstantPixelProjection(parameters, pixelProjectionMap);
        }
    }

    // PointOperator::initialise() step 2
    @Override
    protected Product createTargetProduct() throws OperatorException {
        final Product targetProduct = super.createTargetProduct();

        // should the new bands be created here or in step 3?

        final Band nadirViewLatitudeBand = targetProduct.addBand("latitude_nadir", ProductData.TYPE_FLOAT32);
        nadirViewLatitudeBand.setDescription("Latitude, nadir view");
        nadirViewLatitudeBand.setUnit("deg");
        setNoDataValues(nadirViewLatitudeBand);
        final Band nadirViewLongitudeBand = targetProduct.addBand("longitude_nadir", ProductData.TYPE_FLOAT32);
        nadirViewLongitudeBand.setDescription("Longitude, nadir view");
        nadirViewLongitudeBand.setUnit("deg");
        setNoDataValues(nadirViewLongitudeBand);
        final Band nadirViewTimesBand = targetProduct.addBand("acquisition_time_nadir", ProductData.TYPE_FLOAT32);
        nadirViewTimesBand.setDescription("Acquisition time, nadir");
        nadirViewTimesBand.setUnit("Days since 1-1-2000 (MJD 2000)");
        setNoDataValues(nadirViewTimesBand);

        final Band forwardViewLatitudeBand = targetProduct.addBand("latitude_fward", ProductData.TYPE_FLOAT32);
        forwardViewLatitudeBand.setDescription("Latitude, forward view");
        forwardViewLatitudeBand.setUnit("deg");
        setNoDataValues(forwardViewLatitudeBand);
        final Band forwardViewLongitudeBand = targetProduct.addBand("longitude_fward", ProductData.TYPE_FLOAT32);
        forwardViewLongitudeBand.setDescription("Longitude, forward view");
        forwardViewLongitudeBand.setUnit("deg");
        setNoDataValues(forwardViewLongitudeBand);
        final Band forwardViewTimesBand = targetProduct.addBand("acquisition_time_fward", ProductData.TYPE_FLOAT32);
        forwardViewTimesBand.setDescription("Acquisition time, fward");
        forwardViewTimesBand.setUnit("Days since 1-1-2000 (MJD 2000)");
        setNoDataValues(forwardViewTimesBand);

        if (enableFOV) {
            targetProduct.addBand("Nadir View Pixel FOV Along Track", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("Nadir View Pixel FOV Across Track", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("Forward View Pixel FOV Along Track", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("Forward View Pixel FOV Across Track", ProductData.TYPE_FLOAT32);
        }

        //change target product dimensions if necessary
        return targetProduct;
    }

    private static void setNoDataValues(Band band) {
        band.setNoDataValue(-999999.0);
        band.setNoDataValueUsed(true);
    }

    // PointOperator::initialise() step 3
    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyBands();
        productConfigurer.getTargetProduct().setAutoGrouping("nadir:fward");
    }

    // PointOperator::initialise() step 4
    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        // we dont need the source sample - all the new bands are created from metadata.
        //sampleConfigurer.defineComputedSample(0, sourceProduct.getBandAt(0));
        //sampleConfigurer.defineComputedSample(1, sourceProduct.getBandAt(1));
    }

    // PointOperator::initialise() step 5
    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, "latitude_nadir");
        sampleConfigurer.defineSample(1, "longitude_nadir");
        sampleConfigurer.defineSample(2, "acquisition_time_nadir");

        sampleConfigurer.defineSample(3, "latitude_fward");
        sampleConfigurer.defineSample(4, "longitude_fward");
        sampleConfigurer.defineSample(5, "acquisition_time_fward");

        if (enableFOV) {
            sampleConfigurer.defineSample(6, "Nadir View Pixel FOV Along Track");
            sampleConfigurer.defineSample(7, "Nadir View Pixel FOV Across Track");
            sampleConfigurer.defineSample(8, "Forward View Pixel FOV Along Track");
            sampleConfigurer.defineSample(9, "Forward View Pixel FOV Across Track");
        }
    }

    // Called from PixelOperator::computeTileStack()
    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        int pixel = PIXELS_PER_ROW - 1 - x;
        int[] pixelRelativeNumbers = {0, 0};
        double[] pixelNewPositionsAndTimes = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        Calculator.getPixelPositionsAcquisitionTimes(y, pixel,
                                                     this.s0,
                                                     this.NADIR_VIEW_SCAN_PIX_NUM_ADS_Records,
                                                     this.FWARD_VIEW_SCAN_PIX_NUM_ADS_Records,
                                                     this.SCAN_PIXEL_X_AND_Y_ADS_Records,
                                                     this.GEOLOCATION_ADS_Records,
                                                     this.scanYCoords,
                                                     pixelNewPositionsAndTimes,
                                                     pixelRelativeNumbers,
                                                     this.parameters);

        targetSamples[0].set(pixelNewPositionsAndTimes[0]);
        targetSamples[1].set(pixelNewPositionsAndTimes[1]);
        targetSamples[2].set(pixelNewPositionsAndTimes[2]);

        targetSamples[3].set(pixelNewPositionsAndTimes[3]);
        targetSamples[4].set(pixelNewPositionsAndTimes[4]);
        targetSamples[5].set(pixelNewPositionsAndTimes[5]);

        if (enableFOV) {
            targetSamples[6].set(pixelProjectionMap.get(pixelRelativeNumbers[0]).get(0));
            targetSamples[7].set(pixelProjectionMap.get(pixelRelativeNumbers[0]).get(1));
            targetSamples[8].set(pixelProjectionMap.get(pixelRelativeNumbers[1]).get(0));
            targetSamples[9].set(pixelProjectionMap.get(pixelRelativeNumbers[1]).get(1));
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AatsrUngriddingOp.class);
        }
    }

    private void prepareInputParameters() {
        this.parameters = new InputParameters();

        // Traceability to original command line arguments...

        //0	<aatsr-product> 	"./l1b_sample.n1"	(A)ATSR-1/2 product
        // Not required - input file is passed in as @SourceProduct

        //1	<l1b-characterisation-file> 	"./CH1_Files/ATS_CH1_AX"	L1B characterisation file
        if (L1BCharacterisationFile != null) {
            parameters.parseCharacterisationFile(this.L1BCharacterisationFile.getPath());
        } else {
            parameters.firstForwardPixel = DEFAULT_FIRST_FORWARD_PIXEL;
            parameters.firstNadirPixel = DEFAULT_FIRST_NADIR_PIXEL;
        }
        //parameters.parseCharacterisationFile("C:\\dev\\GBT-UBT-Tool v1.6\\CH1_Files\\ATS_CH1_AX");
        // Sets the following:
        //        parameters.firstForwardPixel;
        //        parameters.firstNadirPixel;

        //2	<fov-measurement-file> 	"./FOV_measurements/10310845.SFV"	AATSR FOV calibration measurement file
        if (enableFOV) {
            parameters.parseRawIFOV(this.FOVMeasurementFile.getPath());
        }
        //parameters.parseRawIFOV("C:\\dev\\GBT-UBT-Tool v1.6\\FOV_measurements\\10310845.SFV");
        // Sets the following:
        //        parameters.alongTrackAngle;
        //        parameters.acrossTrackAngle;
        //        parameters.ifov1D;

        //3	<output-file(.h5/.nc)> 	"./output.nc"	Output file: either hdf5 or netcdf4 cf depending on file extension
        // Not required - output file is passed in as @TargetProduct

        //4	<rows-per-CPU-thread> 	"2000" 	rows assigned per CPU thread
        // Not used since toolbox is responsible for managing computation e.g. in tiles.

        //5	<IFOV-reporting-extent-fraction> 	"0.4" 	extent of IFOV to report as distance in pixel projection
        parameters.pixelIFOVReportingExtent = this.pixelIFOVReportingExtent;

        //6	<Trim-end-of-product> 	"TRUE" 	boolean to allow user to trim image rows of product where no ADS available
        // Output product should have same dimensions as input
        //parameters.trimProductEndWhereNoADS = false;

        //7	<Pixel Reference> 	"Corner" 	to where the pixel coordinates are reference Centre/Corner
        // use default reference i.e. from Corner
        parameters.cornerReferenceFlag = this.cornerReferenceFlag;

        //8	<Topography> 	"FALSE" 	boolean to apply topographic corrections to tie points
        parameters.topographicFlag = this.topographicFlag;

        //9	<Topo-relation> 	"0.05" 	distance (image coordinates) pixel can be from tie-point to have topo correction applied
        parameters.topographyHomogenity = this.topographyHomogenity;

        //10	<Ortho> 	"TRUE"	boolean to orthorectify using an external DEM
        //Disable orthorectification since this is considered to be a different operation
        //parameters.orthorectify = false;

        //11	<DEM> 	"./DEM/global/gt30_global.tif" 	location of external Global DEM GeoTIFF
        // Note dummy parameter must be passed if orthorectify set FALSE.
        //parameters.DEMFilename = "Not used";

        //12	OPT<[ix,iy]> 	"[0,0]" 	optional argument to convert single pixel ix, iy
        //13	OPT<[jx,jy]>"	"[511,511]"	optional argument used with args[11?] to convert array of pixels [ix,iy], [jx,jy]
        // Disable subsetting so output product will have same dimensions as input
        //parameters.subsetFlag = false;
        //parameters.singlePixelFlag = false;
        // not used:
        //        parameters.x1;
        //        parameters.x2;
        //        parameters.y1;
        //        parameters.y2;
    }

    private void prepareMetadata() {
        // Get the ADS from the product
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        this.NADIR_VIEW_SCAN_PIX_NUM_ADS_Records = metadataRoot.getElement("NADIR_VIEW_SCAN_PIX_NUM_ADS").getElementGroup();
        this.FWARD_VIEW_SCAN_PIX_NUM_ADS_Records = metadataRoot.getElement("FWARD_VIEW_SCAN_PIX_NUM_ADS").getElementGroup();
        this.SCAN_PIXEL_X_AND_Y_ADS_Records = metadataRoot.getElement("SCAN_PIXEL_X_AND_Y_ADS").getElementGroup();
        this.GEOLOCATION_ADS_Records = metadataRoot.getElement("GEOLOCATION_ADS").getElementGroup();

        // Get list of GeoLocation ADS scanYCoords (Seems to be very expensive so only compute once)
        this.scanYCoords = new ArrayList<>();
        for (int k = 0; k < GEOLOCATION_ADS_Records.getNodeCount(); k++) {
            MetadataElement GeoAdsRecord = GEOLOCATION_ADS_Records.get(k);
            scanYCoords.add(GeoAdsRecord.getAttributeDouble("img_scan_y"));
        }

        // Get the scan number of the first record of the scanPixelADS
        MetadataElement firstRecord = SCAN_PIXEL_X_AND_Y_ADS_Records.get(0);
        MetadataAttribute instr_scan_num = firstRecord.getAttributeAt(2);
        ProductData data = instr_scan_num.getData();
        this.s0 = data.getElemInt(); // = 32
    }

/*    private void prepareSubset(){
        // Get the dimensions of the product using one of the bands
        Band band = sourceProduct.getBand("btemp_nadir_1200");
        MultiLevelImage sourceImage = band.getSourceImage();
        int maxXValue;
        int maxY;
        int minXValue = 0;
        int minYValue = 0;
        // If subseting the product, set the min/max dimensions according to input
        if (parameters.subsetFlag) {
            maxXValue = parameters.x2;
            maxY = parameters.y2;
            minXValue = parameters.x1;
            minYValue = parameters.y1;
        } else {
            maxXValue = sourceImage.getMaxX();
            maxY = sourceImage.getMaxY();
        }
        final int maxX = maxXValue;
        final int minX = minXValue;
        final int minY = minYValue;
        if (maxX > sourceImage.getMaxX()) {
            System.out.println("Check input X coordinate");
        }
        if (maxY > sourceImage.getMaxY()) {
            System.out.println("Check input Y coordinate");
        }
        if (parameters.trimProductEndWhereNoADS && !parameters.subsetFlag) {
            // If true, trim product for image rows where no ADS is available
            // (ATSR-1/2 have less image rows than ADS cover)
            int a = 32 * SCAN_PIXEL_X_AND_Y_ADS_Records.getNodeCount() + 32;
            int b = 32 * GEOLOCATION_ADS_Records.getNodeCount() + 32;
            int c = 32 * NADIR_VIEW_SCAN_PIX_NUM_ADS_Records.getNodeCount() + 32;
            int d = 32 * FWARD_VIEW_SCAN_PIX_NUM_ADS_Records.getNodeCount() + 32;
            maxY = GeolocationInterpolator.getMinValue(a, b, c, d);
            if (maxY > sourceImage.getMaxY()){
                maxY = sourceImage.getMaxY();
            }
            System.out.println("Number of image rows covered by ADS: "+maxY+" / "+sourceImage.getMaxY());
        }
    }*/
}
