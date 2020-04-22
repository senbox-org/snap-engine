package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
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
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

    @Test
    public void testInstall_withGlob() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir, targetDir);
        resourceInstaller.install("glob:**/*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("org/esa/snap/core/dataio/dimap/spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

    @Test
    public void testMustInstallResource_modifiedTime() throws Exception {
        Path origFiles = targetDir.resolve("origs");
        Files.createDirectories(origFiles);
        ResourceInstaller originalInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), origFiles);
        originalInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(origFiles).toArray().length);

        Path newFiles = targetDir.resolve("new");
        Files.createDirectories(newFiles);
        ResourceInstaller newFilesInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), newFiles);
        newFilesInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(newFiles).toArray().length);

        Path file2 = newFiles.resolve("file-2.txt");
        Thread.sleep(2200);
        long updateTime = Files.getLastModifiedTime(file2).toMillis() + 2000;
        Files.setLastModifiedTime(file2, FileTime.fromMillis(updateTime));

        ResourceInstaller updateInstaller = new ResourceInstaller(newFiles, origFiles);
        updateInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(origFiles).toArray().length);

        Path updateFile2 = origFiles.resolve("file-2.txt");
        assertEquals(updateTime, Files.getLastModifiedTime(updateFile2).toMillis());

    }

    @Test
    public void testMustInstallResource_modifiedSize() throws Exception {
        Path origFiles = targetDir.resolve("origs");
        Files.createDirectories(origFiles);
        ResourceInstaller originalInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), origFiles);
        originalInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(origFiles).toArray().length);

        Path newFiles = targetDir.resolve("new");
        Files.createDirectories(newFiles);
        ResourceInstaller newFilesInstaller = new ResourceInstaller(sourceDir.resolve("resource-testdata/auxdata"), newFiles);
        newFilesInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(newFiles).toArray().length);

        Path file2 = newFiles.resolve("file-2.txt");
        try (OutputStream outputStream = Files.newOutputStream(file2, StandardOpenOption.APPEND)) {
            outputStream.write(1234);
        }
        ResourceInstaller updateInstaller = new ResourceInstaller(newFiles, origFiles);
        updateInstaller.install(".*txt", ProgressMonitor.NULL);
        assertEquals(4, Files.list(origFiles).toArray().length);

        Path updateFile2 = origFiles.resolve("file-2.txt");
        assertEquals(Files.size(file2), Files.size(updateFile2));

    }

    /*
    @Test
    public void testResourcesFromDir() throws Exception {
        URL resource = ResourceInstallerTest.class.getResource("/resource-testdata");
        assertNotNull(resource);
        testResourcesFromClassLoader(new URLClassLoader(new URL[]{resource}));
    }

    @Test
    public void testResourcesFromJar() throws Exception {
        URL resource = ResourceInstallerTest.class.getResource("/resource-testdata.jar");
        assertNotNull(resource);
        testResourcesFromClassLoader(new URLClassLoader(new URL[]{resource}));
    }

    private void testResourcesFromClassLoader(ClassLoader cl) throws URISyntaxException, IOException {
        Enumeration<URL> auxdata = cl.getResources("/auxdata");
        while (auxdata.hasMoreElements()) {
            URL url = auxdata.nextElement();
            System.out.println("url = " + url);
        }

        URL auxdataDirUrl = cl.getResource("auxdata");
        assertNotNull(auxdataDirUrl);
        Path auxdataDir = Paths.get(auxdataDirUrl.toURI());
        assertTrue(Files.isDirectory(auxdataDir));

        List<Path> auxdataDirList = Files.list(auxdataDir).collect(Collectors.toList());
        Collections.sort(auxdataDirList);
        assertEquals(2, auxdataDirList.size());
        assertEquals("file-1.txt", auxdataDirList.get(0).getFileName().toString());
        assertEquals("file-2.txt", auxdataDirList.get(1).getFileName().toString());
        assertEquals("file-3.txt", auxdataDirList.get(2).getFileName().toString());
        assertEquals("subdir", auxdataDirList.get(3).getFileName().toString());

        URL auxdataSubdirUrl = cl.getResource("/auxdata/subdir");
        assertNotNull(auxdataSubdirUrl);
        Path auxdataSubdir = Paths.get(auxdataSubdirUrl.toURI());
        assertTrue(Files.isDirectory(auxdataSubdir));

        List<Path> auxdataSubdirList = Files.list(auxdataSubdir).collect(Collectors.toList());
        Collections.sort(auxdataSubdirList);
        assertEquals(3, auxdataSubdirList.size());
        assertEquals("file-A.txt", auxdataSubdirList.get(0).getFileName().toString());
        assertEquals("file-B.txt", auxdataSubdirList.get(1).getFileName().toString());
        assertEquals("file-C.txt", auxdataSubdirList.get(2).getFileName().toString());
    }
*/
}
