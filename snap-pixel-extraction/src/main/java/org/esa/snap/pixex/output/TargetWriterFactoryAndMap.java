package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TargetWriterFactoryAndMap {

    private static final String MEASUREMENTS_FILE_NAME_PATTERN = "%s_%s_measurements.txt";

    private final Map<String, PrintWriter> writerMap;
    private final String filenamePrefix;
    private final Path outputDir;

    public TargetWriterFactoryAndMap(final String filenamePrefix, final Path outputDir) {
        writerMap = new HashMap<String, PrintWriter>();
        this.filenamePrefix = filenamePrefix;
        this.outputDir = outputDir;
    }

    public boolean containsWriterFor(Product product) {
        return writerMap.containsKey(product.getProductType());
    }

    public PrintWriter getWriterFor(Product product) {
        return writerMap.get(product.getProductType());
    }

    public PrintWriter createWriterFor(Product product) throws IOException {
        String productType = product.getProductType();
        final String fileName = String.format(MEASUREMENTS_FILE_NAME_PATTERN, filenamePrefix, productType);
        Path coordinateFile = outputDir.resolve(fileName);
        PrintWriter writer = new PrintWriter(Files.newOutputStream(coordinateFile));
        writerMap.put(productType, writer);
        return writer;
    }

    public void close() {
        final Collection<PrintWriter> writerCollection = writerMap.values();
        for (PrintWriter printWriter : writerCollection) {
            printWriter.close();
            //noinspection SuspiciousMethodCalls
//            writerMap.remove(printWriter);
        }
    }
}
