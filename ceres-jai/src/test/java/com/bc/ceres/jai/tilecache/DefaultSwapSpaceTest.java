package com.bc.ceres.jai.tilecache;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DefaultSwapSpaceTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_deletesStoredSwapFiles() throws Exception {
        Path swapDir = IOHelper.createATempDir("swap-space-test");
        try (DefaultSwapSpace swapSpace = new DefaultSwapSpace(swapDir)) {
            BufferedImage owner = new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_GRAY);
            Raster raster = owner.getData();
            MemoryTile memoryTile = new MemoryTile(owner, 0, 0, raster, null);

            assertTrue(swapSpace.storeTile(memoryTile));

            try (Stream<Path> stream = Files.walk(swapDir)) {
                assertTrue(stream.anyMatch(Files::isRegularFile));
            }

            swapSpace.close();

            try (Stream<Path> stream = Files.walk(swapDir)) {
                assertEquals(0L, stream.filter(Files::isRegularFile).count());
            }
        } finally {
            IOHelper.deleteFileTree(swapDir);
        }
    }
}