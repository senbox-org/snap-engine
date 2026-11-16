/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.main;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.core.PrintWriterConciseProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.subset.AbstractSubsetRegion;
import org.esa.snap.core.subset.GeometrySubsetRegion;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.converters.RectangleConverter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The default command line context.
 */
class DefaultCommandLineContext implements CommandLineContext {

    @Override
    public Product readProduct(String productFilepath) throws IOException {
        return readProduct(productFilepath, null);
    }

    @Override
    public Product readProduct(String productFilePath, Map<String, String> readParams) throws IOException {
        final Path input = Path.of(productFilePath);
        if (!Files.exists(input)) {
            throw new OperatorException("'" + productFilePath + "' file didn't exist");
        }
        final ProductReader productReader = ProductIO.getProductReaderForInput(input);
        if (productReader == null) {
            throw new OperatorException("No product reader found for '" + productFilePath + "'");
        }
        ProductSubsetDef subsetDef = null;
        if (readParams != null && (readParams.containsKey("pixelRegion") || readParams.containsKey("geometryRegion"))) {
            subsetDef = new ProductSubsetDef();
            AbstractSubsetRegion subsetRegion = null;
            if (readParams.containsKey("geometryRegion")) {
                try {
                    subsetRegion = new GeometrySubsetRegion(new JtsGeometryConverter().parse(readParams.get("geometryRegion")), 0);
                } catch (ConversionException e) {
                    throw new IOException(e);
                }
            } else if (readParams.containsKey("pixelRegion")) {
                try {
                    subsetRegion = new PixelSubsetRegion(new RectangleConverter().parse(readParams.get("pixelRegion")), 0);
                } catch (ConversionException e) {
                    throw new IOException(e);
                }
            }
            subsetDef.setSubsetRegion(subsetRegion);
        }
        Product product = productReader.readProductNodes(input, subsetDef);
        if (product.getProductReader() == null) {
            product.setProductReader(productReader);
        }
        return product;
    }

    @Override
    public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
        GPF.writeProduct(targetProduct, Path.of(filePath), formatName, clearCacheAfterRowWrite, false, new PrintWriterConciseProgressMonitor(System.out));
    }

    @Override
    public Graph readGraph(String filePath, Map<String, String> templateVariables) throws GraphException, IOException {
        Graph graph;
        try (Reader fileReader = createReader(filePath)) {
            graph = GraphIO.read(fileReader, templateVariables);
        }
        return graph;
    }

    @Override
    public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
        GraphProcessor processor = new GraphProcessor();
        if (observer != null) {
            processor.addObserver(observer);
        }
        processor.executeGraph(graph, new PrintWriterConciseProgressMonitor(System.out));
    }

    @Override
    public void print(String m) {
        System.out.print(m);
    }

    @Override
    public Logger getLogger() {
        return SystemUtils.LOG;
    }

    @Override
    public Reader createReader(String textFilePath) throws IOException {
        return Files.newBufferedReader(Path.of(textFilePath));
    }

    @Override
    public Writer createWriter(String fileName) throws IOException {
        return Files.newBufferedWriter(Path.of(fileName));
    }

    @Override
    public String[] list(String path) throws IOException {
        Path directory = Path.of(path);
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            return IOHelper.list(directory).toArray(new String[0]);
        } else {
            return null;
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        return Files.exists(Path.of(fileName));
    }

    @Override
    public boolean isFile(String path) {
        return Files.isRegularFile(Path.of(path));
    }
}
