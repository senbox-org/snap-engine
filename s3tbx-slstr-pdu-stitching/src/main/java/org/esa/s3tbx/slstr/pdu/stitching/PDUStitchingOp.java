package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "PduStitching",
        category = "Optical",
        version = "1.0",
        authors = "Tonio Fincke",
        copyright = "Copyright (C) 2015 by Brockmann Consult (info@brockmann-consult.de)",
        description = "Stitches multiple SLSTR L1B product dissemination units (PDUs) of the same orbit to a single product.",
        autoWriteDisabled = true)
public class PDUStitchingOp extends Operator {

    @SourceProducts(description = "The product dissemination units to be stitched together. Must all be of type 'SLSTR L1B'.\n" +
            "If not given, the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the product dissemination units.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).\n" +
            "If not given, the parameter 'sourceProducts' must be provided.")
    private String[] sourceProductPaths;

    @Parameter(description = "The directory to which the stitched product shall be written.\n" +
            "Within this directory, a folder of the SLSTR L1B naming format will be created.\n" +
            "If no target directory is given, the product will be written to the user directory.")
    private File targetDir;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        if ((sourceProducts == null || sourceProducts.length == 0) &&
                (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException("Either 'sourceProducts' pr 'sourceProductPaths' must be set");
        }
        final Set<File> filesByProduct = getSourceProductsFileSet(sourceProducts);
        final Set<File> filesByPath = getSourceProductsPathFileSet(sourceProductPaths, getLogger());
        filesByPath.addAll(filesByProduct);
        final File[] files = filesByPath.toArray(new File[filesByPath.size()]);
        if (files.length == 0) {
            throw new OperatorException("No PDUs to be stitched could be found.");
        }
        if (targetDir == null || StringUtils.isNullOrEmpty(targetDir.getAbsolutePath())) {
            targetDir = new File(SystemUtils.getUserHomeDir().getPath());
        }
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDir, files, ProgressMonitor.NULL);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private static Set<File> getSourceProductsFileSet(Product[] sourceProducts) {
        Set<File> sourceProductFileSet = new TreeSet<>();
        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                sourceProductFileSet.add(sourceProduct.getFileLocation());
            }
        }
        return sourceProductFileSet;
    }

    //todo copied this from pixexop - move to utililty method? - tf 20151117
    public static Set<File> getSourceProductsPathFileSet(String[] sourceProductPaths, Logger logger) {
        Set<File> sourceProductFileSet = new TreeSet<>();
        String[] paths = trimSourceProductPaths(sourceProductPaths);
        if (paths != null && paths.length != 0) {
            for (String path : paths) {
                try {
                    WildcardMatcher.glob(path, sourceProductFileSet);
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                }
            }
            if (sourceProductFileSet.isEmpty()) {
                logger.log(Level.WARNING, "No valid source product path found.");
            }
        }
        return sourceProductFileSet;
    }

    //todo copied this from pixexop - move to utililty method? - tf 20151117
    private static String[] trimSourceProductPaths(String[] sourceProductPaths) {
        final String[] paths;
        if (sourceProductPaths != null) {
            paths = sourceProductPaths.clone();
        } else {
            paths = null;
        }
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].trim();
            }
        }
        return paths;
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PDUStitchingOp.class);
        }
    }

}
