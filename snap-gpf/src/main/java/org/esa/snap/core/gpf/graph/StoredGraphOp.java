package org.esa.snap.core.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;


/**
 * Created by obarrile on 11/08/2016.
 */
@OperatorMetadata(alias = "StoredGraph",
        description = "Encapsulates an stored graph into an operator.",
        internal = true,
        autoWriteDisabled = true)
public class StoredGraphOp extends Operator {

    @Parameter(description = "The file from which the graph is read.", notNull = true, notEmpty = true)
    private File file;

    GraphProcessor processor = null;
    GraphContext graphContext = null;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        if (file == null) {
            throw new OperatorException("'file' parameter must be set");
        }
        try {
            FileReader reader = new FileReader(file);
            Graph graph = GraphIO.read(reader);

            processor = new GraphProcessor();
            graphContext = new GraphContext(graph);
        } catch (GraphException e) {
            throw new OperatorException(e);
        } catch (FileNotFoundException e) {
            throw new OperatorException("'file' not found");
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) {
        processor.executeGraph(graphContext, ProgressMonitor.NULL);
    }

    @Override
    public void dispose() {
        graphContext.dispose();
        super.dispose();
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StoredGraphOp.class);
        }
    }

}
