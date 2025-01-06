package org.esa.snap.performance.actions;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.performance.util.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadBeforeExecutionAction implements Action, NestedAction {

    private final String productName;
    private final String testDataDir;
    private final Action nestedAction;
    private List<Result> allResults;

    public ReadBeforeExecutionAction(Action nestedAction, String productName, String testDataDir) {
        this.productName = productName;
        this.testDataDir = testDataDir;
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws IOException {
        this.allResults = new ArrayList<>();
        Action readProduct = new SimpleReadProductAction(this.productName, this.testDataDir);
        readProduct.execute();
        Product product = (Product) readProduct.fetchResults().get(0).getValue();

        injectProductIntoWriteAction(this.nestedAction, product);

        this.nestedAction.execute();
        product.dispose();

        List<Result> results = this.nestedAction.fetchResults();
        this.allResults.addAll(results);
    }

    @Override
    public void cleanUp() {
        this.nestedAction.cleanUp();
    }

    @Override
    public List<Result> fetchResults() {
        return this.allResults;
    }

    private void injectProductIntoWriteAction(Action action, Product product) {
        if (action instanceof WriteAction) {
            ((WriteAction) action).setProduct(product);
        } else if (action instanceof NestedAction) {
            injectProductIntoWriteAction(((NestedAction) action).getNestedAction(), product);
        }
    }

    @Override
    public Action getNestedAction() {
        return this.nestedAction;
    }
}
