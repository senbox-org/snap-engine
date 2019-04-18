package org.esa.snap.core.gpf.execution;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.StringUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Operators {

    @OperatorMetadata(alias = "InitComputeTile")
    public static class InitComputeTileOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("initComputeTile", "initComputeTile", 1, 1);
            product.addBand("band1", ProductData.TYPE_INT8);
            setTargetProduct(product);
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            targetTile.setSample(0, 0, 3);
        }

    }

    static class InitComputeTileOperatorSpi extends OperatorSpi {

        InitComputeTileOperatorSpi() {
            super(InitComputeTileOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitDoExecuteComputeTile")
    public static class InitDoExecuteComputeTileOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        private int summand;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("initDoExecuteComputeTile", "initDoExecuteComputeTile", 1, 1);
            product.addBand("band1", ProductData.TYPE_INT8);
            summand = 0;
            setTargetProduct(product);
        }

        @Override
        public void doExecute(ProgressMonitor pm) throws OperatorException {
            summand += 2;
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            targetTile.setSample(0, 0, 1 + summand);
        }
    }

    static class InitDoExecuteComputeTileOperatorSpi extends OperatorSpi {

        InitDoExecuteComputeTileOperatorSpi() {
            super(InitDoExecuteComputeTileOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitJAIImage")
    public static class InitJAIImageOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("initJAIImage", "initJAIImage", 1, 1);
            Number[] bandValues = new Byte[]{3};
            RenderedOp constantImage = ConstantDescriptor.create(1f, 1f, bandValues, null);
            Band band1 = product.addBand("band1", ProductData.TYPE_INT8);
            band1.setSourceImage(constantImage);
            setTargetProduct(product);
        }

    }

    static class InitJAIImageOperatorSpi extends OperatorSpi {

        InitJAIImageOperatorSpi() {
            super(InitJAIImageOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitJAIImageDoExecute")
    public static class InitJAIImageDoExecuteOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("initJAIImageDoExecute", "initJAIImageDoExecute", 1, 1);
            Number[] bandValues = new Byte[]{3};
            RenderedOp constantImage = ConstantDescriptor.create(1f, 1f, bandValues, null);
            Band band1 = product.addBand("band1", ProductData.TYPE_INT8);
            band1.setSourceImage(constantImage);
            setTargetProduct(product);
        }

        @Override
        public void doExecute(ProgressMonitor pm) throws OperatorException {
            Product targetProduct = getTargetProduct();
            Number[] bandValues = new Byte[]{3};
            RenderedOp constantImage = ConstantDescriptor.create(1f, 1f, bandValues, null);
            Band band = targetProduct.addBand("band2", ProductData.TYPE_INT8);
            band.setSourceImage(constantImage);
        }

    }

    static class InitJAIImageDoExecuteOperatorSpi extends OperatorSpi {

        InitJAIImageDoExecuteOperatorSpi() {
            super(InitJAIImageDoExecuteOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitDoExecuteSetsJAIImage")
    public static class InitDoExecuteSetsJAIImageOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("initDoExecuteSetsJAIImage", "initDoExecuteSetsJAIImage", 1, 1);
            product.addBand("band1", ProductData.TYPE_INT8);
            setTargetProduct(product);
        }

        @Override
        public void doExecute(ProgressMonitor pm) throws OperatorException {
            Product targetProduct = getTargetProduct();
            Band band1 = targetProduct.getBand("band1");
            Number[] bandValues = new Byte[]{3};
            RenderedOp constantImage = ConstantDescriptor.create(1f, 1f, bandValues, null);
            band1.setSourceImage(constantImage);
        }

    }

    static class InitDoExecuteSetsJAIImageOperatorSpi extends OperatorSpi {

        InitDoExecuteSetsJAIImageOperatorSpi() {
            super(InitDoExecuteSetsJAIImageOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitDoExecuteSetsTargetProductAndJAIImage")
    public static class InitDoExecuteSetsTargetProductAndJAIImageOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            // do nothing
            Product dummyProduct = new Product("dummy", "dummy", 1, 1);
            setTargetProduct(dummyProduct);
        }

        @Override
        public void doExecute(ProgressMonitor pm) throws OperatorException {
            Product product = new Product("initDoExecuteSetsTargetProductAndJAIImage",
                    "initDoExecuteSetsTargetProductAndJAIImage", 1, 1);
            Band band1 = product.addBand("band1", ProductData.TYPE_INT8);
            Number[] bandValues = new Byte[]{3};
            RenderedOp constantImage = ConstantDescriptor.create(1f, 1f, bandValues, null);
            band1.setSourceImage(constantImage);
            setTargetProduct(product);
        }

    }

    static class InitDoExecuteSetsTargetProductAndJAIImageOperatorSpi extends OperatorSpi {

        InitDoExecuteSetsTargetProductAndJAIImageOperatorSpi() {
            super(InitDoExecuteSetsTargetProductAndJAIImageOperator.class);
        }
    }


    @OperatorMetadata(alias = "InitAndDoExecuteSetNoTargetProduct")
    public static class InitAndDoExecuteSetNoTargetProductOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Parameter
        String outputFilePath;

        @Override
        public void initialize() throws OperatorException {
            assert(StringUtils.isNotNullAndNotEmpty(outputFilePath));
            Product dummyProduct = new Product("dummy", "dummy", 1, 1);
            setTargetProduct(dummyProduct);
        }

        @Override
        public void doExecute(ProgressMonitor pm) throws OperatorException {
            try {
                PrintWriter printWriter = new PrintWriter(new File(outputFilePath));
                printWriter.print("something");
                printWriter.close();
            } catch (FileNotFoundException e) {
                throw new OperatorException(e.getMessage());
            }
        }

    }

    static class InitAndDoExecuteSetNoTargetProductOperatorSpi extends OperatorSpi {

        InitAndDoExecuteSetNoTargetProductOperatorSpi() {
            super(InitAndDoExecuteSetNoTargetProductOperator.class);
        }
    }

    @OperatorMetadata(alias = "InitSetsNoTargetProduct")
    public static class InitSetsNoTargetProductOperator extends Operator {

        @TargetProduct
        Product targetProduct;

        @Parameter
        String outputFilePath;

        @Override
        public void initialize() throws OperatorException {
            assert(StringUtils.isNotNullAndNotEmpty(outputFilePath));
            try {
                PrintWriter printWriter = new PrintWriter(new File(outputFilePath));
                printWriter.print("something");
                printWriter.close();
            } catch (FileNotFoundException e) {
                throw new OperatorException(e.getMessage());
            }
            Product dummyProduct = new Product("dummy", "dummy", 1, 1);

            setTargetProduct(dummyProduct);
        }

    }

    static class InitSetsNoTargetProductOperatorSpi extends OperatorSpi {

        InitSetsNoTargetProductOperatorSpi() {
            super(InitSetsNoTargetProductOperator.class);
        }
    }

}
