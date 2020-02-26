package org.esa.snap.lib.openjpeg.header;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by jcoravu on 7/6/2019.
 */
public class BufferedRandomAccessFileTest {

    @Test
    public void testReadFileContent() throws Exception {
        File testJP2File = JP2FileReaderTest.getTestDataDir("sample.jp2");
        Assert.assertNotNull(testJP2File);

        Path filePath = testJP2File.toPath();
        Assert.assertNotNull(filePath);

        Assert.assertTrue("The input test file '"+filePath.toString()+"' does not exist.", Files.exists(filePath));

        BufferedRandomAccessFile bufferedRandomAccessFile = new BufferedRandomAccessFile(filePath, 1024, true);

        Assert.assertEquals(16298, bufferedRandomAccessFile.getLength());

        Assert.assertEquals(0, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(0, bufferedRandomAccessFile.readShort());

        Assert.assertEquals(12, bufferedRandomAccessFile.readShort());

        Assert.assertEquals(106, bufferedRandomAccessFile.readByte());

        Assert.assertEquals(5, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(1344282637, bufferedRandomAccessFile.readInt());

        Assert.assertEquals(2695, bufferedRandomAccessFile.readUnsignedShort());

        Assert.assertEquals(11, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(1.6259746672639677E-260d, bufferedRandomAccessFile.readDouble(), 0.0d);

        Assert.assertEquals(2.9022051E29f, bufferedRandomAccessFile.readFloat(), 0.0f);

        Assert.assertEquals(23, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(2305843009220669490L, bufferedRandomAccessFile.readLong());

        Assert.assertEquals(536870913L, bufferedRandomAccessFile.readUnsignedInt());

        byte[] buffer = new byte[100];
        bufferedRandomAccessFile.read(buffer, 0, buffer.length);

        Assert.assertEquals(135, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(89, buffer[0]);
        Assert.assertEquals(106, buffer[1]);
        Assert.assertEquals(112, buffer[2]);
        Assert.assertEquals(50, buffer[3]);

        bufferedRandomAccessFile.seek(1000);

        Assert.assertEquals(1000, bufferedRandomAccessFile.getPosition());

        Assert.assertEquals(4800905805561501015L, bufferedRandomAccessFile.readLong());

        Assert.assertEquals(-1352857162, bufferedRandomAccessFile.readInt());

        Assert.assertEquals(158, bufferedRandomAccessFile.readByte());

        Assert.assertEquals(720965248, bufferedRandomAccessFile.readUnsignedInt());

        Assert.assertEquals(-32640, bufferedRandomAccessFile.readShort());

        Assert.assertEquals(1019, bufferedRandomAccessFile.getPosition());
    }
}
