package org.esa.s3tbx.dataio.s3;

import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * Dummy class so that Sentinel3ProductReader can be instantiated.
 * @author Norman Fomferra
 */
public class MockUpSentinel3ProductReaderPlugIn implements ProductReaderPlugIn {
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (new File(input.toString()).getName().equals("pom.xml")) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MockUpSentinel3ProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"SENTINEL-3-SLSTR"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".xml"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "Sentinel-3 SLSTR Data Product";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0],
                                  getDefaultFileExtensions()[0],
                                  getDescription(null));
    }
}
