package org.esa.snap.jp2.reader;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.jp2.reader.JP2ProductReaderPlugin;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by jcoravu on 5/11/2019.
 */
public class JP2ProductReaderTest {

    public JP2ProductReaderTest() {
    }

    @Test
    public void testColorModel() throws Exception {
        File imageFile = getTestDataDir("sample.jp2");
        assertNotNull(imageFile);

        Product snapProduct = new JP2ProductReaderPlugin().createReaderInstance().readProductNodes(imageFile, null);
        assertNotNull(snapProduct);

        Rectangle part = new Rectangle(0, 0, 10, 10);
        MultiLevelImage multiLevelImage = snapProduct.getBands()[0].getSourceImage();
        assertNotNull(multiLevelImage);
        assertNotNull(multiLevelImage.getColorModel());
        assertNotNull(multiLevelImage.getData());
        assertNotNull(multiLevelImage.getBounds());

        BufferedImage bufferedImage = multiLevelImage.getAsBufferedImage(part, multiLevelImage.getColorModel());
        assertNotNull(bufferedImage);
    }

    private static File getTestDataDir() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./snap-jp2-reader/src/test/data/");
            if (!dir.exists()) {
                fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    private static File getTestDataDir(String path) {
        return new File(getTestDataDir(), path);
    }

}
