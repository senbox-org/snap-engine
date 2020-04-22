package org.esa.snap.lib.openjpeg.header;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by jcoravu on 7/6/2019.
 */
public class JP2FileReaderTest {

    @Test
    public void testReadFileHeader() throws Exception {
        File testJP2File = JP2FileReaderTest.getTestDataDir("space.jp2");
        Assert.assertNotNull(testJP2File);

        Path filePath = testJP2File.toPath();
        Assert.assertNotNull(filePath);

        Assert.assertTrue("The input test file '"+filePath.toString()+"' does not exist.", Files.exists(filePath));

        JP2FileReader jp2FileReader = new JP2FileReader();
        jp2FileReader.readFileFormat(filePath, 1024, true);

        List<String> xmlMetadata = jp2FileReader.getXmlMetadata();

        Assert.assertNotNull(xmlMetadata);
        Assert.assertEquals(2, xmlMetadata.size());

        String firstXML = xmlMetadata.get(0);
        Assert.assertNotNull(firstXML);
        Assert.assertEquals(377, firstXML.length());

        String secondXML = xmlMetadata.get(1);
        Assert.assertNotNull(secondXML);
        Assert.assertEquals(902, secondXML.length());

        ContiguousCodestreamBox contiguousCodestreamBox = jp2FileReader.getHeaderDecoder();
        Assert.assertNotNull(contiguousCodestreamBox);

        SIZMarkerSegment siz = contiguousCodestreamBox.getSiz();
        Assert.assertNotNull(siz);
        Assert.assertEquals(1, siz.computeNumTilesX());
        Assert.assertEquals(1, siz.computeNumTilesY());
        Assert.assertEquals(1, siz.computeNumTiles());
        Assert.assertEquals(400, siz.getImageHeight());
        Assert.assertEquals(700, siz.getImageWidth());
        Assert.assertEquals(0, siz.getImageLeftX());
        Assert.assertEquals(0, siz.getImageTopY());
        Assert.assertEquals(0, siz.getTileLeftX());
        Assert.assertEquals(0, siz.getTileTopY());
        Assert.assertEquals(1, siz.getNumComps());
        Assert.assertEquals(8, siz.getComponentOriginBitDepthAt(0));
        Assert.assertEquals(400, siz.getCompImgHeight(0));
        Assert.assertEquals(700, siz.getCompImgWidth(0));
        Assert.assertEquals(1, siz.getComponentDxAt(0));
        Assert.assertEquals(1, siz.getComponentDyAt(0));
        Assert.assertEquals(false, siz.isComponentOriginSignedAt(0));

        CODMarkerSegment cod = contiguousCodestreamBox.getCod();
        Assert.assertNotNull(cod);
        Assert.assertEquals(0, cod.getMultipleComponenTransform());
        Assert.assertEquals(6, cod.getCodeBlockCount());
        Assert.assertEquals(64, cod.getCodeBlockHeight());
        Assert.assertEquals(64, cod.getCodeBlockWidth());
        Assert.assertEquals(0, cod.getCodeBlockStyle());
        Assert.assertEquals(0, cod.getCodingStyle());
        Assert.assertEquals(1, cod.getNumberOfLayers());
        Assert.assertEquals(0, cod.getProgressiveOrder());
        Assert.assertEquals(1, cod.getQmfbid());
        Assert.assertEquals(15, cod.getCodeBlockHeightExponentOffset(0));
        Assert.assertEquals(15, cod.getCodeBlockWidthExponentOffset(0));

        QCDMarkerSegment qcd = contiguousCodestreamBox.getQcd();
        Assert.assertNotNull(qcd);
        Assert.assertEquals(2, qcd.getNumGuardBits());
        Assert.assertEquals(0, qcd.getQuantizationType());
        Assert.assertEquals(6, qcd.getResolutionLevels());
        Assert.assertEquals(4, qcd.getSubbandsAtResolutionLevel(0));
        Assert.assertEquals(0, qcd.computeExponent(0, 0));
        Assert.assertEquals(64, qcd.computeMantissa(0, 0));
        Assert.assertEquals(8, qcd.computeNoQuantizationExponent(0, 0));

        RGNMarkerSegment rgn = contiguousCodestreamBox.getRgn();
        Assert.assertNull(rgn);
    }

    private static File getTestDataDir() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./lib-openjpeg/src/test/data/");
            if (!dir.exists()) {
                Assert.fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    public static File getTestDataDir(String path) {
        return new File(getTestDataDir(), path);
    }
}
