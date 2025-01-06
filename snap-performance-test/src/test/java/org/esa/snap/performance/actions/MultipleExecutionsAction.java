package org.esa.snap.performance.actions;

import org.esa.snap.performance.util.Parameters;
import org.esa.snap.performance.util.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultipleExecutionsAction implements Action, NestedAction {

    private final Action nestedAction;
    private final Parameters params;
    private List<Result> results;

    public MultipleExecutionsAction(Action nestedAction, Parameters params) {
        this.nestedAction = nestedAction;
        this.params = params;
        this.results = new ArrayList<>();
    }

    @Override
    public void execute() throws IOException {
        boolean discardFirstMeasure = this.params.isDiscardFirstMeasure();
        int numExecutions = this.params.getNumExecutionsForAverageOperations();
        if (discardFirstMeasure) {
            numExecutions++;
        }

        for (int ii = 0; ii < numExecutions; ii++) {
            this.nestedAction.execute();
            if (ii == 0 && discardFirstMeasure) {
                this.nestedAction.cleanUp();
                continue;
            }
            List<Result> iterationResults = new ArrayList<>(this.nestedAction.fetchResults());
            this.results.addAll(iterationResults);
            nestedAction.cleanUp();
        }
    }

    @Override
    public void cleanUp() {
        this.results = new ArrayList<>();
    }

    @Override
    public List<Result> fetchResults() {
        return this.results;
    }

    @Override
    public Action getNestedAction() {
        return this.nestedAction;
    }
}
