package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.test.LongTestRunner;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
@RunWith(LongTestRunner.class)
public class ResourceInstallerTest {

    private Path sourceDir;
    private Path targetDir;

    @Before
    public void setUp() throws Exception {
        sourceDir = ResourceInstaller.findModuleCodeBasePath(ResourceInstallerTest.class);

        FileSystem targetFs = Jimfs.newFileSystem();
        targetDir = targetFs.getPath("test");
        Files.createDirectories(targetDir);
    }

    @After
    public void tearDown() throws Exception {
        targetDir.getFileSystem().close();
    }

    @Test
    public void testInstall() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir.resolve("org/esa/snap/core/dataio/dimap"), targetDir);
        resourceInstaller.install(".*xml", ProgressMonitor.NULL);
        assertEquals(1, IOHelper.listFiles(targetDir).size());
        assertEquals(4, IOHelper.listFiles(targetDir.resolve("spi")).size());
    }

    @Test
    public void testInstall_withGlob() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir, targetDir);
        resourceInstaller.install("glob:**/*xml", ProgressMonitor.NULL);
        assertEquals(1, IOHelper.listFiles(targetDir).size());
        assertEquals(4, IOHelper.list(targetDir.resolve("org/esa/snap/core/dataio/dimap/spi")).size());
    }

    @Test
    public void testMustInstallResource_modifiedTime() throws Exception {
        Path origFiles = targetDir.resolve("origs");
        Files.createDirectories(origFiles);
        ResourceInstaller originalInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), origFiles);
        originalInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(origFiles).size());

        Path newFiles = targetDir.resolve("new");
        Files.createDirectories(newFiles);
        ResourceInstaller newFilesInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), newFiles);
        newFilesInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(newFiles).size());

        Path file2 = newFiles.resolve("file-2.txt");
        Thread.sleep(2200);
        long updateTime = Files.getLastModifiedTime(file2).toMillis() + 2000;
        Files.setLastModifiedTime(file2, FileTime.fromMillis(updateTime));

        ResourceInstaller updateInstaller = new ResourceInstaller(newFiles, origFiles);
        updateInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(origFiles).size());

        Path updateFile2 = origFiles.resolve("file-2.txt");
        assertEquals(updateTime, Files.getLastModifiedTime(updateFile2).toMillis());

    }

    @Test
    public void testMustInstallResource_modifiedSize() throws Exception {
        Path origFiles = targetDir.resolve("origs");
        Files.createDirectories(origFiles);
        ResourceInstaller originalInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), origFiles);
        originalInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(origFiles).size());

        Path newFiles = targetDir.resolve("new");
        Files.createDirectories(newFiles);
        ResourceInstaller newFilesInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), newFiles);
        newFilesInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(newFiles).size());

        Path file2 = newFiles.resolve("file-2.txt");
        try (OutputStream outputStream = Files.newOutputStream(file2, StandardOpenOption.APPEND)) {
            outputStream.write(1234);
        }
        ResourceInstaller updateInstaller = new ResourceInstaller(newFiles, origFiles);
        updateInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, IOHelper.listFiles(origFiles).size());

        Path updateFile2 = origFiles.resolve("file-2.txt");
        assertEquals(Files.size(file2), Files.size(updateFile2));

    }
}
