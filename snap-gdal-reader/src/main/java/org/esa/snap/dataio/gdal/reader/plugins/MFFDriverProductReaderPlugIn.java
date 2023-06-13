package org.esa.snap.dataio.gdal.reader.plugins;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.StringUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class MFFDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public MFFDriverProductReaderPlugIn() {
        super(".hdr", "MFF", "Vexcel MFF Raster");
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        DecodeQualification result = super.getDecodeQualification(input);

        if (DecodeQualification.UNABLE != result) {
            File inputFile = null;
            if (input instanceof String) {
                inputFile = new File((String)input);
            } else if (input instanceof File) {
                inputFile = (File)input;
            } else if (input instanceof Path) {
                inputFile = ((Path)input).toFile();
            } else {
                throw new IllegalArgumentException("Unknown type '"+input.getClass()+"' for input '"+ input.toString()+"'.");
            }
            String filePath = inputFile.getAbsolutePath();
            // '.rat.hdr' file extension for RATProductReaderPlugIn
            // 'bin.hdr' file extension for PolsarProProductReaderPlugIn
            // '.snaphu.hdr' file extension for SNAPHUProductReaderPlugIn
            if (StringUtils.endsWithIgnoreCase(filePath, ".rat.hdr") || StringUtils.endsWithIgnoreCase(filePath, "bin.hdr")
                    || StringUtils.endsWithIgnoreCase(filePath, ".snaphu.hdr")) {

                result = DecodeQualification.UNABLE;
            } else {
                // '.hdr, .dbl' file extensions for SmosProductReaderPlugIn
                boolean canContinue = true;
                if (StringUtils.endsWithIgnoreCase(filePath, ".hdr")) {
                    File hdrFile = FileUtils.exchangeExtension(inputFile, ".hdr");
                    File dblFile = FileUtils.exchangeExtension(inputFile, ".dbl");
                    if (hdrFile.exists() && dblFile.exists()) {
                        canContinue = false;
                    }
                }
                if (canContinue) {
                    // open the input file as an image stream to check the header for EnviProductReaderPlugIn
                    try {
                        ImageInputStream headerStream = new FileImageInputStream(inputFile);
                        result = checkDecodeQualificationOnStream(headerStream);
                    } catch (IOException e) {
                        result = DecodeQualification.UNABLE;
                    }
                } else {
                    result = DecodeQualification.UNABLE;
                }
            }
        }
        return result;
    }

    private static DecodeQualification checkDecodeQualificationOnStream(ImageInputStream headerStream) throws IOException {
        String line = headerStream.readLine();
        if (line == null || !line.startsWith("ENVI")) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }
}
