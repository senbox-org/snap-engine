/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.lib.openjpeg.jp2;

import org.esa.snap.lib.openjpeg.jp2.boxes.FileTypeBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.ImageHeaderBox;
import org.esa.snap.lib.openjpeg.jp2.boxes.Jpeg2000SignatureBox;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class BoxReaderS2Test {

    @Test
    public void testIsoSpecPart1ConformanceFiles() throws IOException, URISyntaxException {
        test("/org/esa/snap/lib/openjpeg/dataio/s2/l1c/S2A_OPER_MSI_L1C_TL_CGS1_20130621T120000_A000065_T14SLD_B02.jp2", 10980, 10980);
    }

    private void test(String jp2Path, int width, int height) throws URISyntaxException, IOException {
        final BoxReader boxReader = openBoxReader(jp2Path);

        final Box box1 = boxReader.readBox();
        Assert.assertEquals(BoxType.decode4b("jP  "), box1.getCode());
        assertEquals(12, box1.getLength());
        Assert.assertEquals(0x0D0A870A, ((Jpeg2000SignatureBox) box1).getSignature());

        final Box box2 = boxReader.readBox();
        Assert.assertEquals(BoxType.decode4b("ftyp"), box2.getCode());
        assertEquals(20, box2.getLength());
        final FileTypeBox fileTypeBox = (FileTypeBox) box2;
        assertEquals(BoxType.decode4b("jp2 "), fileTypeBox.getBr());
        assertEquals(0, fileTypeBox.getMinV());
        Assert.assertEquals("jp2 ", BoxType.encode4b(fileTypeBox.getCl0()));

        final Box box3 = boxReader.readBox();
        Assert.assertEquals(BoxType.decode4b("jp2h"), box3.getCode());

        final Box box4 = boxReader.readBox();
        Assert.assertEquals(BoxType.decode4b("ihdr"), box4.getCode());
        assertEquals(22, box4.getLength());
        final ImageHeaderBox imageHeaderBox = (ImageHeaderBox) box4;
        assertEquals(height, imageHeaderBox.getHeight());
        assertEquals(width, imageHeaderBox.getWidth());

    }

     static BoxReader openBoxReader(String jp2Path) throws URISyntaxException, IOException {
        final File file = new File(BoxReaderS2Test.class.getResource(jp2Path).toURI());
        final FileImageInputStream stream = new FileImageInputStream(file);
        return new BoxReader(stream, file.length(), new MyListener());
    }


    private static class MyListener implements BoxReader.Listener {
        @Override
        public void knownBoxSeen(Box box) {
            System.out.println("known box: " + BoxType.encode4b(box.getCode()));
        }

        @Override
        public void unknownBoxSeen(Box box) {
            System.out.println("unknown box: " + BoxType.encode4b(box.getCode()));
        }
    }
}
