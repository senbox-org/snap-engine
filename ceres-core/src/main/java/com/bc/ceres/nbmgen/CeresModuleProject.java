package com.bc.ceres.nbmgen;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Norman
 */
class CeresModuleProject {

    public static final String ORIGINAL_POM_XML = "pom-original.xml";
    public static final String POM_XML = "pom.xml";

    final Path projectDir;

    final Path pomFile;
    final Document pomDocument;

    final Path moduleFile;
    final Document moduleDocument;

    interface Processor {
        void process(CeresModuleProject project) throws JDOMException, IOException;
    }

    static void processParentDir(Path parentDir, Processor processor) throws JDOMException, IOException {
        List<Path> moduleDirs = CeresModuleProject.getCeresModuleDirs(parentDir);
        if (moduleDirs.isEmpty()) {
            throw new IOException("no modules found in " + parentDir);
        }
        for (Path moduleDir : moduleDirs) {
            processor.process(CeresModuleProject.create(moduleDir));
        }
    }

    private CeresModuleProject(Path projectDir, Path pomFile, Document pomDocument, Path moduleFile, Document moduleDocument) {
        this.projectDir = projectDir;
        this.pomFile = pomFile;
        this.pomDocument = pomDocument;
        this.moduleFile = moduleFile;
        this.moduleDocument = moduleDocument;
    }

    private static boolean isCeresModuleProjectDir(Path file) {
        return Files.isDirectory(file)
                && Files.exists(getPomFile(file))
                && Files.exists(getCeresModuleFile(file));
    }

    private static CeresModuleProject create(Path projectDir) throws JDOMException, IOException {

        Path originalPomFile = getFile(projectDir, ORIGINAL_POM_XML);
        Path sourcePomFile;
        if (!Files.exists(originalPomFile)) {
            sourcePomFile = getFile(projectDir, POM_XML);
        } else {
            sourcePomFile = originalPomFile;
        }
        Document pomDocument = readXml(sourcePomFile);

        Path moduleFile = getCeresModuleFile(projectDir);
        Document moduleDocument = readXml(moduleFile);

        return new CeresModuleProject(projectDir, sourcePomFile, pomDocument, moduleFile, moduleDocument);
    }

    private static Path getPomFile(Path file) {
        return getFile(file, "pom.xml");
    }

    private static Path getCeresModuleFile(Path file) {
        return getFile(file, "src", "main", "resources", "module.xml");
    }

    static List<Path> getCeresModuleDirs(Path parentDir) {
        Collection<Path> moduleDirs = IOHelper.listFiles(parentDir, CeresModuleProject::isCeresModuleProjectDir);
        if (moduleDirs != null) {
            return moduleDirs.stream().toList();
        }
        return Collections.emptyList();
    }

    static Path getFile(Path dir, String... names) {
        Path file = dir;
        for (String name : names) {
            file = file.resolve(name);
        }
        return file;
    }

    private static Document readXml(Path file) throws JDOMException, IOException {
        return new SAXBuilder().build(Files.newInputStream(file));
    }
}
