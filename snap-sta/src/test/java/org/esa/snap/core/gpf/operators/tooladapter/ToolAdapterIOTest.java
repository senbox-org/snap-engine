package org.esa.snap.core.gpf.operators.tooladapter;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipFile;

public class ToolAdapterIOTest extends TestCase {

    public void testGetFileNameMapping_withRootDir() throws Exception {
        // Test with root_dir.zip (has a common root directory)
        URL rootDirUrl = getClass().getResource("root_dir.zip");
        assertNotNull("Test file root_dir.zip not found", rootDirUrl);
        File rootDirFile = new File(rootDirUrl.toURI());
        try (ZipFile rootDirZip = new ZipFile(rootDirFile)) {
            // Get the mapping
            Map<String, String> rootDirMapping = ToolAdapterIO.getFileNameMapping(rootDirZip);

            // Verify the mapping is not empty
            assertEquals("There should be two entries", 3, rootDirMapping.size());

            assertEquals("", rootDirMapping.get("root/"));
            assertEquals("dir1/", rootDirMapping.get("root/dir1/"));
            assertEquals("dir2/", rootDirMapping.get("root/dir2/"));
        }

    }

    public void testGetFileNameMapping_withSingleDirs() throws Exception {
        // Test with single_dirs.zip (no common root directory)
        URL singleDirsUrl = getClass().getResource("single_dirs.zip");
        assertNotNull("Test file single_dirs.zip not found", singleDirsUrl);
        File singleDirsFile = new File(singleDirsUrl.toURI());

        try (ZipFile singleDirsZip = new ZipFile(singleDirsFile)) {
            // Get the mapping
            Map<String, String> singleDirsMapping = ToolAdapterIO.getFileNameMapping(singleDirsZip);

            // Verify the mapping is not empty
            assertEquals("There should be two entries", 2, singleDirsMapping.size());

            assertEquals("dir1/", singleDirsMapping.get("dir1/"));
            assertEquals("dir2/", singleDirsMapping.get("dir2/"));

        }

    }
}