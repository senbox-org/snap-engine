package org.esa.snap.performance.actions;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MeasureTimeAction implements Action, NestedAction {

    private final String UNIT = "ms";
    private final Action nestedAction;
//    private Double result;
    private List<Result> allResults;

    public MeasureTimeAction(Action nestedAction) {
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws IOException {
        this.allResults = new ArrayList<>();
        StopWatch watch = new StopWatch();
        watch.start();
        this.nestedAction.execute();
        watch.stop();

        List<Result> results = this.nestedAction.fetchResults();
        results.add(new Result(ActionName.MEASURE_TIME.getName(), true, watch.getTimeDiff(), this.UNIT));
        this.allResults = results;
    }

    @Override
    public void cleanUp() {
        this.nestedAction.cleanUp();
    }

    @Override
    public List<Result> fetchResults() {
        return this.allResults;
    }

    @Override
    public Action getNestedAction() {
        return this.nestedAction;
    }
}
