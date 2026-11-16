package org.esa.snap.core.gpf.common;

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * test ProductSetReader usage in GPF
 */
public class ProductSetReaderOpTest {

    private static Path outputFile;

    @BeforeClass
    public static void setup() {
        outputFile = GlobalTestConfig.getSnapTestDataOutputFile("ProductSetReaderOpTest/writtenProduct.dim");
        IOHelper.createDirectories(outputFile.getParent());
    }

    @AfterClass
    public static void teardown() {
        FileUtils.deleteTree(outputFile.getParent());
    }

    @Test
    public void testProductSetGraph() throws IOException, GraphException {
        final String graphPath = getTestFilePath("/org/esa/snap/core/gpf/common/productset/ProductSetMosaicGraph.xml");
        final Path product1 = Path.of(getTestFilePath("/org/esa/snap/core/gpf/common/productset/subset1.dim"));
        final Path product2 = Path.of(getTestFilePath("/org/esa/snap/core/gpf/common/productset/subset2.dim"));
        final Path[] srcFiles = new Path[] { product1, product2 };

        Graph graph;
        try (Reader fileReader = Files.newBufferedReader(Path.of(graphPath))) {
            graph = GraphIO.read(fileReader);
        }

        final GraphProcessor processor = new GraphProcessor();
        setIO(graph, srcFiles, outputFile, "BEAM-DIMAP");

        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    @Test
    public void testFolderProductSetGraph() throws IOException, GraphException {
        final String graphPath = getTestFilePath("/org/esa/snap/core/gpf/common/productset/ProductSetMosaicGraph.xml");
        final Path product1 = Path.of(getTestFilePath("/org/esa/snap/core/gpf/common/productset/subset1.dim"));
        final Path[] srcFiles = new Path[] { product1.getParent() };

        Graph graph;
        try (Reader fileReader = Files.newBufferedReader(Path.of(graphPath))) {
            graph = GraphIO.read(fileReader);
        }

        final GraphProcessor processor = new GraphProcessor();
        setIO(graph, srcFiles, outputFile, "BEAM-DIMAP");

        processor.executeGraph(graph, ProgressMonitor.NULL);
    }

    private void setIO(final Graph graph, final Path[] srcFiles, final Path tgtFile, final String format) {
        final String readOperatorAlias = OperatorSpi.getOperatorAlias(ProductSetReaderOp.class);
        final Node readerNode = findNode(graph, readOperatorAlias);
        if (readerNode != null) {
            final DomElement param = new DefaultDomElement("parameters");
            String value = "";
            for(Path srcFile : srcFiles) {
                value += srcFile.toAbsolutePath() +",";
            }
            param.createChild("fileList").setValue(value);
            readerNode.setConfiguration(param);
        }

        final String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);
        final Node writerNode = findNode(graph, writeOperatorAlias);
        if (writerNode != null && tgtFile != null) {
            final DomElement origParam = writerNode.getConfiguration();
            origParam.getChild("file").setValue(tgtFile.toAbsolutePath().toString());
            if (format != null)
                origParam.getChild("formatName").setValue(format);
        }
    }

    private static Node findNode(final Graph graph, final String alias) {
        for (Node n : graph.getNodes()) {
            if (n.getOperatorName().equals(alias))
                return n;
        }
        return null;
    }

    private String getTestFilePath(String name) {
        URL url = ProductSetReaderOpTest.class.getResource(name);
        Assert.assertNotNull(url);
        final Path filePath = UrlHelper.urlToFile(url);
        Assert.assertNotNull(filePath);
        return filePath.toString();
    }
}
