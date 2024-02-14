package org.esa.snap.dataio.gdal.reader.plugins;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.dataio.geotiff.Utils;

import java.nio.file.Path;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class GTiffDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public GTiffDriverProductReaderPlugIn() {
        //super("GTiff", "GeoTIFF");
        // make a difference between GDAL GeoTIFF and SNAP GeoTIFF reader
        // in case this driver is enabled from GDAL
        super("GTiff", "GeoTIFF (GDAL)");

        addExtension(".tif");
        addExtension(".tiff");
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final DecodeQualification qualification = super.getDecodeQualification(input);
        if (qualification == DecodeQualification.UNABLE) {
            return qualification;
        }
        final Path filePath = getInput(input);
        try {
            // 2020-07-21 CC Added COG check
            return Utils.isCOGGeoTIFF(filePath) ? DecodeQualification.INTENDED : DecodeQualification.SUITABLE;
        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
    }
}
