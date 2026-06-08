package org.esa.snap.dataio.gdal.reader.plugins;

import org.esa.snap.core.dataio.DecodeQualification;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class SNAPTiffDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public SNAPTiffDriverProductReaderPlugIn() {
        // make a difference between GDAL GeoTIFF and SNAP GeoTIFF reader
        // in case this driver is enabled from GDAL
        super("SNAP_TIFF", "SNAP GeoTIFF (GDAL)");

        addExtension(".tif");
        addExtension(".tiff");
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final DecodeQualification qualification = super.getDecodeQualification(input);
        if (qualification == DecodeQualification.UNABLE) {
            return qualification;
        }
        return DecodeQualification.SUITABLE;
    }
}
