package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
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

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).", notNull = true)
    String[] sourceProductPaths;

    @Parameter(description = "The directory to which the stitched product shall be written. Within this directory, a folder" +
            "of the SLSTR L1B naming format will be created. If no target directory is given, the product will be written to" +
            "the user directory.")
    private File targetDir;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        final Set<File> fileSet = getSourceProductFileSet(sourceProductPaths, getLogger());
        final File[] files = fileSet.toArray(new File[fileSet.size()]);
        if (files.length == 0) {
            return;
        }
        if (StringUtils.isNullOrEmpty(targetDir.getAbsolutePath())) {
            targetDir = new File(SystemUtils.getUserHomeDir().getPath());
        }
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDir, files);
        } catch (IOException | PDUStitchingException | ParserConfigurationException | TransformerException e) {
            throw new OperatorException(e.getMessage());
        }
    }

    //todo copied this from pixexop - move to utililty method? - tf 20151117
    public static Set<File> getSourceProductFileSet(String[] sourceProductPaths, Logger logger) {
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
