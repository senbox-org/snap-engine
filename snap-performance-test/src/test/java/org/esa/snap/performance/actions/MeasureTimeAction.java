package org.esa.snap.performance.actions;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.Result;

import java.util.ArrayList;
import java.util.List;

public class MeasureTimeAction implements Action, NestedAction {

    private final String UNIT = "s";
    private final Action nestedAction;
    private List<Result> allResults;

    public MeasureTimeAction(Action nestedAction) {
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws Throwable {
        this.allResults = new ArrayList<>();
        StopWatch watch = new StopWatch();

        watch.start();
        this.nestedAction.execute();
        watch.stop();

        double timeInSeconds = watch.getTimeDiff() / 1000.0;

        List<Result> results = this.nestedAction.fetchResults();
        results.add(new Result(ActionName.MEASURE_TIME.getName(), true, timeInSeconds, this.UNIT));
        this.allResults = results;

        System.out.println("TIME: " + timeInSeconds + "s");
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
