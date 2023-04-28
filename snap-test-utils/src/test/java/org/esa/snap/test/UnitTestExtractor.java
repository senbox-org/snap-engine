package org.esa.snap.test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Iterates recursively over a given project directory and its submodules scanning for surefire test report
 * files (TEST-***.xml) contained in {@code target/surefire-reports}.
 * It generates a csv table file with the scanned project directory and the creation date in the header comment (prefixed with #).
 * The table has the following columns: {@code Module; Package; Class; Method}
 */
public class UnitTestExtractor {

    public static void main(String[] args) throws IOException {
        Path projectDir = validateParameter(args);
        TreeMap<String, TestInfo> testMap = new TreeMap<>();
        Files.walkFileTree(projectDir, new TestFileVisitor(testMap));

        if (testMap.isEmpty()) {
            throw new IllegalStateException(String.format("No tests files found in '%s'", projectDir));
        }

        Path outFile = projectDir.resolve("tests.csv");
        try (PrintStream os = new PrintStream(Files.newOutputStream(outFile))) {
            TestInfo.printHeader(os, projectDir);

            testMap.values().forEach(info -> info.print(os));
        }
    }

    private static Path validateParameter(String[] args) {
        if (args.length < 1) {
            throw new IllegalStateException("Missing project directory parameter");
        }

        Path projectDir = Paths.get(args[0]);
        if (!Files.exists(projectDir)) {
            throw new IllegalArgumentException(String.format("Path '%s' does not exist.", projectDir));
        }
        if (!Files.isDirectory(projectDir)) {
            throw new IllegalArgumentException(String.format("Path '%s' is not a directory.", projectDir));
        }
        return projectDir;
    }

    private static class TestFileVisitor extends SimpleFileVisitor<Path> {

        private final TreeMap<String, TestInfo> moduleTestInfoMap;
        private final DocumentBuilder builder;
        private final XPathFactory xPathfactory;

        public TestFileVisitor(TreeMap<String, TestInfo> testMap) {
            this.moduleTestInfoMap = testMap;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Could not create XML parser factory", e);
            }
            xPathfactory = XPathFactory.newInstance();

        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (dir.endsWith("src")) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = file.getFileName().toString().toLowerCase();
            if (!(fileName.startsWith("test-") && fileName.endsWith(".xml"))) {
                return FileVisitResult.CONTINUE;
            }

            try (InputStream is = Files.newInputStream(file)) {
                Document doc = builder.parse(is);
                XPath xpath = xPathfactory.newXPath();
                String moduleName = file.getParent().getParent().getParent().getFileName().toString();
                XPathExpression tcExpr = xpath.compile("testsuite/testcase");
                NodeList nl = (NodeList) tcExpr.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < nl.getLength(); i++) {
                    Node testNode = nl.item(i);
                    TestInfo moduleTestInfo = moduleTestInfoMap.computeIfAbsent(moduleName, s -> new TestInfo(moduleName));
                    String classname = testNode.getAttributes().getNamedItem("classname").getNodeValue();
                    String testName = testNode.getAttributes().getNamedItem("name").getNodeValue();
                    List<String> methodList = moduleTestInfo.classMethodNames.computeIfAbsent(classname, s -> new ArrayList<>());
                    methodList.add(testName);
                }
            } catch (SAXException | XPathExpressionException e) {
                throw new IOException(String.format("Not able to parse file %s", file), e);
            }

            return FileVisitResult.CONTINUE;
        }
    }


    private static class TestInfo {
        String moduleName;
        Map<String, List<String>> classMethodNames = new TreeMap<>();

        public TestInfo(String moduleName) {
            this.moduleName = moduleName;
        }

        private static void printHeader(PrintStream os, Path projectDir) {
            os.printf("# Scanned Project Directory: %s%n", projectDir.toAbsolutePath());
            LocalDateTime creationDate = LocalDateTime.now();
            os.printf("# Time of Creation: %s%n", creationDate.format(DateTimeFormatter.ISO_DATE_TIME));
            printRow(os, "Module", "Package", "Class", "Method");
            os.flush();
        }

        private static void printRow(PrintStream os, String moduleName, String packageName, String className, String methodName) {
            os.printf("%s;%s;%s;%s%n", moduleName, packageName, className, methodName);
        }

        private void print(PrintStream os) {
            for (Map.Entry<String, List<String>> entry : classMethodNames.entrySet()) {
                String testClass = entry.getKey();
                String className = testClass.substring(testClass.lastIndexOf(".") + 1);
                String packageName = testClass.substring(0, testClass.lastIndexOf("."));
                List<String> testMethods = entry.getValue();
                for (String methodName : testMethods) {
                    printRow(os, moduleName, packageName, className, methodName);
                }
            }
            os.flush();
        }
    }
}
