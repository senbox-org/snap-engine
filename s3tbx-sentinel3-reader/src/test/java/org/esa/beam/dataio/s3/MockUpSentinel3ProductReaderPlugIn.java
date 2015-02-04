package org.esa.beam.dataio.s3;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

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
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0],
                                  getDefaultFileExtensions()[0],
                                  getDescription(null));
    }
}
