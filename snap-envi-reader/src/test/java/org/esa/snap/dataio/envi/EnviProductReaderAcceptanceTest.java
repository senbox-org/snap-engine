package org.esa.snap.dataio.envi;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.fail;

public class EnviProductReaderAcceptanceTest {

    public void not_____testReadCorrectHeaderImgPair() throws IOException {
        final StringBuffer headerContent = new StringBuffer();
        headerContent.append("ENVI\n");
        headerContent.append("sensor type = Unknown\n");

        writeHeaderFile(headerContent);

        final byte[] imageContent = new byte[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                1, 2, 3, 4
        };
        writeImageFile(imageContent);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader productReader = plugIn.createReaderInstance();
        final Product product = productReader.readProductNodes(headerFile, null);
        // @todo 2 tb/** what exactly does this test check for???
    }

    @Test
    public void testReadHeader_ImgMissing() throws IOException {
        StringBuffer headerContent = new StringBuffer();

        writeHeaderFile(headerContent);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader productReader = plugIn.createReaderInstance();
        try {
            final Product product = productReader.readProductNodes(headerFile, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private Path headerFile;
    private Path imageFile;

    @Before
    public void setUp() throws Exception {
        headerFile = null;
        imageFile = null;
    }

    @After
    public void tearDown() throws Exception {
        if (headerFile != null) {
            if (!Files.deleteIfExists(headerFile)) {
                fail("unable to delete: " + headerFile.toRealPath());
            }
        }
        if (imageFile != null) {
            if (!Files.deleteIfExists(imageFile)) {
                fail("unable to delete: " + imageFile.toRealPath());
            }
        }
    }

    private void writeHeaderFile(StringBuffer headerContent) throws IOException {
        headerFile = IOHelper.createATempDir().resolve("test_1.hdr");
        IOHelper.createFile(headerFile);

        final OutputStream headerOut = Files.newOutputStream(headerFile);
        headerOut.write(headerContent.toString().getBytes());
        headerOut.flush();
        headerOut.close();
    }

    private void writeImageFile(byte[] content) throws IOException {
        imageFile = IOHelper.createATempDir().resolve("test_1.img");
        IOHelper.createFile(imageFile);

        final OutputStream headerOut = Files.newOutputStream(imageFile);
        headerOut.write(content);
        headerOut.flush();
        headerOut.close();
    }
}
