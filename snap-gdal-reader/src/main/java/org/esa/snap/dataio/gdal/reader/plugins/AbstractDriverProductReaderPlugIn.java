package org.esa.snap.dataio.gdal.reader.plugins;

import org.esa.snap.dataio.gdal.reader.GDALMetadataInspector;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public abstract class AbstractDriverProductReaderPlugIn implements ProductReaderPlugIn {
    private final String driverName;
    private final String driverDisplayName;
    private final String pluginFormatName;
    protected final Set<String> extensions;


    protected AbstractDriverProductReaderPlugIn(String driverName, String driverDisplayName) {
        this.extensions = new HashSet<>();
        this.driverName = driverName;
        this.driverDisplayName = driverDisplayName;
        this.pluginFormatName = "GDAL-" + driverName + "-READER";
    }

    protected AbstractDriverProductReaderPlugIn(String extension, String driverShortName, String driverLongName) {
        this(driverShortName, driverLongName);

        addExtension(extension);
    }

    @Override
    public MetadataInspector getMetadataInspector() {
        return new GDALMetadataInspector();
    }

    @Override
    public final String getDescription(Locale locale) {
        return this.driverDisplayName;
    }

    @Override
    public final String[] getFormatNames() {
        return new String[] { this.pluginFormatName };
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final String filePath = getInput(input).toString();
        for (String extension : this.extensions) {
            if (StringUtils.endsWithIgnoreCase(filePath, extension)) {
                return DecodeQualification.SUITABLE;
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public final Class[] getInputTypes() {
        return new Class[] { String.class, File.class };
    }

    @Override
    public final ProductReader createReaderInstance() {
        return new GDALProductReader(this);
    }

    @Override
    public final String[] getDefaultFileExtensions() {
        String[] defaultExtensions = new String[this.extensions.size()];
        this.extensions.toArray(defaultExtensions);
        return defaultExtensions;
    }

    @Override
    public final SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(Locale.getDefault()));
    }

    protected final void addExtension(String extension) {
        this.extensions.add(extension);
    }

    protected Path getInput(Object input) {
        if (input instanceof String) {
            return Paths.get((String)input);
        } else if (input instanceof File) {
            return ((File)input).toPath().toAbsolutePath();
        } else if (input instanceof Path) {
            return ((Path)input).toAbsolutePath();
        } else {
            throw new IllegalArgumentException("Unsupported type '" + input.getClass() + "' for input '"+ input.toString() + "'.");
        }
    }

    public final String getDriverName() {
        return driverName;
    }
}

