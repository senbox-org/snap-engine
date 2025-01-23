package org.esa.snap.performance.actions;

import org.esa.snap.performance.util.Result;

import java.util.List;

public interface Action {

    void execute() throws Throwable;
    void cleanUp();
    List<Result> fetchResults();
}
