package org.esa.beam.dataio.s3;

import org.junit.Ignore;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
@Ignore
public class MockUpSentinel3ProductReaderTest {

    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException,
                                                  IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new MockUpSentinel3ProductReader(new MockUpSentinel3ProductReaderPlugIn()).readProductNodes("", null);
    }
}
