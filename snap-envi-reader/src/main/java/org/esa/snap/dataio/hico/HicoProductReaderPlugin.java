package org.esa.snap.dataio.hico;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * Reader plugin for HICO data products.
 * http://hico.coas.oregonstate.edu/datasets/datacharacteristics.shtml
 */
public class HicoProductReaderPlugin implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final Collection<Path> hdrFiles = findHdrFiles(input);
        if (!hdrFiles.isEmpty()) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"HICO"};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new HicoProductReader(this);
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".hdr"};
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, Path.class};
    }

    @Override
    public String getDescription(Locale locale) {
        return "HICO Data Products";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null)) {
            @Override
            public boolean accept(Path file) {
                // directories are accepted right away
                if (Files.isDirectory(file)) {
                    return true;
                }
                String fileName = file.getFileName().toString();
                return fileName.startsWith("iss.") && fileName.endsWith(".hico.hdr");
            }
        };
    }

    static Collection<Path> findHdrFiles(Object input) {
        if (input != null) {
            Path file = Path.of(input.toString());
            String fileName = file.getFileName().toString();
            HicoFilename hicoFilename = HicoFilename.create(fileName);
            if (hicoFilename != null) {
                Path parentFile = file.getParent();
                if (parentFile == null) {
                    parentFile = Path.of(".");
                }
                return hicoFilename.findAllHdrs(parentFile);
            }
        }
        return new ArrayList<>();
    }
}
