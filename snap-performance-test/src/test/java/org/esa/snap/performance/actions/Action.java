package org.esa.snap.performance.actions;

import org.esa.snap.performance.util.Result;

import java.io.IOException;
import java.util.List;

public interface Action {

    void execute() throws IOException;
    void cleanUp();
    List<Result> fetchResults();
}
