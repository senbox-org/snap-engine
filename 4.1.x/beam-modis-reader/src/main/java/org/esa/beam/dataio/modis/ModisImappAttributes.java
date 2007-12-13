package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfGlobalAttributes;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.Dimension;
import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;


class ModisImappAttributes implements ModisGlobalAttributes {

    private final File inFile;
    private final Logger logger;
    private final int sdId;
    private final Dimension productDimension;
    private final HashMap<String, Integer> dimensionMap;
    private final HashMap<String, IncrementOffset> subsamplingMap;

    private String productName;
    private String productType;
    private Date sensingStart;
    private Date sensingStop;


    public ModisImappAttributes(File inFile, int sdId) {
        logger = BeamLogManager.getSystemLogger();
        productDimension = new Dimension(0, 0);
        dimensionMap = new HashMap<String, Integer>(10);
        subsamplingMap = new HashMap<String, IncrementOffset>(10);
        this.inFile = inFile;
        this.sdId = sdId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public Dimension getProductDimensions() {
        return productDimension;
    }

    public boolean isImappFormat() {
        return true;
    }

    public HdfDataField getDatafield(String name) throws ProductIOException {
        HdfDataField result = null;

        final String widthName = name + "_width";
        final String heightName = name + "_height";
        final String layersName = name + "_z";
        Integer width = dimensionMap.get(widthName);
        Integer height = dimensionMap.get(heightName);
        Integer z = dimensionMap.get(layersName);

        if (width != null && height != null) {
            result = new HdfDataField();
            result.setWidth(width);
            result.setHeight(height);
            if (z != null) {
                result.setLayers(z);
            } else {
                result.setLayers(1);
            }
            result.setDimensionNames(new String[]{widthName, heightName, layersName});
            result.setName(name);
        }

        return result;
    }

    public int[] getTiePointSubsAndOffset(String dimensionName) {
        IncrementOffset incrementOffset = subsamplingMap.get(dimensionName);
        if (incrementOffset != null) {
            int[] result = new int[2];
            result[0] = incrementOffset.increment;
            result[1] = incrementOffset.offset;

            return result;
        }
        return null;
    }

    public Date getSensingStart() {
        return sensingStart;
    }

    public Date getSensingStop() {
        return sensingStop;
    }

    public void decode(final HdfGlobalAttributes hdfAttributes) throws ProductIOException {
        parseFileNamendType();
        parseProductDimensions();

        extractStartAndStopTimes(hdfAttributes);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void parseFileNamendType() {
        productName = FileUtils.getFilenameWithoutExtension(inFile);
        final int index = productName.indexOf('.');
        if (index > 0) {
            productType = productName.substring(0, index);
        } else {
            logger.warning("Unable to retrieve the product type from the file name.");
            productType = "unknown";
        }
    }

    private void parseProductDimensions() throws ProductIOException {
        // @todo 1 tb/tb this is a rather crude method to retrieve the product dimension: scan all datasets.
        // Find out if there is a clever and more performant way to do this
        int[] numDatasets = new int[1];

        try {
            HDFLibrary.SDfileinfo(sdId, numDatasets);

            int[] dimSize = new int[3];
            int[] dimInfo = new int[3];
            String[] dimName = {""};
            for (int n = 0; n < numDatasets[0]; n++) {
                final int sdsId = HDFLibrary.SDselect(sdId, n);

                if (!HDFLibrary.SDgetinfo(sdsId, dimName, dimSize, dimInfo)) {
                    final String msg = "Unable to retrieve meta information for dataset '" + dimName[0] + '\'';
                    logger.severe(msg);
                    throw new HDFException(msg);
                }

                final String widthName = dimName[0] + "_width";
                final String heightName = dimName[0] + "_height";
                if (dimSize[2] == 0) {
                    if (productDimension.width < dimSize[1]) {
                        productDimension.width = dimSize[1];
                    }
                    if (productDimension.height < dimSize[0]) {
                        productDimension.height = dimSize[0];
                    }
                    dimensionMap.put(widthName, dimSize[1]);
                    dimensionMap.put(heightName, dimSize[0]);
                } else {
                    if (productDimension.width < dimSize[2]) {
                        productDimension.width = dimSize[2];
                    }
                    if (productDimension.height < dimSize[1]) {
                        productDimension.height = dimSize[1];
                    }
                    dimensionMap.put(widthName, dimSize[2]);
                    dimensionMap.put(heightName, dimSize[1]);
                    dimensionMap.put(dimName[0] + "_z", dimSize[0]);
                }

                ModisUtils.clearDimensionArrays(dimInfo, dimSize);
                addTiePointOffsetAndSubsampling(sdsId, widthName, heightName);

                HDFLibrary.SDendaccess(sdsId);
            }
        } catch (HDFException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    private void addTiePointOffsetAndSubsampling(int sdsId, String widthName, String heightName) throws HDFException {
        String lineNumbers = ModisUtils.getNamedStringAttribute(sdsId, "line_numbers");
        IncrementOffset incrementOffset;
        if (StringUtils.isNotNullAndNotEmpty(lineNumbers)) {
            incrementOffset = ModisUtils.getIncrementOffset(lineNumbers);
            subsamplingMap.put(heightName, incrementOffset);

        }
        String frameNumbers = ModisUtils.getNamedStringAttribute(sdsId, "frame_numbers");
        if (StringUtils.isNotNullAndNotEmpty(frameNumbers)) {
            incrementOffset = ModisUtils.getIncrementOffset(frameNumbers);
            subsamplingMap.put(widthName, incrementOffset);
        }
    }

    private void extractStartAndStopTimes(HdfGlobalAttributes hdfAttributes) throws ProductIOException {
        try {
            final String startDate = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_BEGIN_DATE_KEY);
            final String startTime = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_BEGIN_TIME_KEY);
            final String endDate = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_END_DATE_KEY);
            final String endTime = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_END_TIME_KEY);

            if (startDate == null || startTime == null) {
                logger.warning("Unable to retrieve sensing start time from metadata");
                sensingStart = null;
                //throw new ProductIOException("Unable to retrieve sensing start time from metadata");
            } else {
                sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);
            }

            if (endDate == null || endTime == null) {
                logger.warning("Unable to retrieve sensing stop time from metadata");
                sensingStop = null;
                //throw new ProductIOException("Unable to retrieve sensing stop time from metadata");
            } else {
                sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
            }
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
