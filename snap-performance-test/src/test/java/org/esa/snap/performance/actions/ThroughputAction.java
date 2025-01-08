package org.esa.snap.performance.actions;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.performance.util.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ThroughputAction implements Action, NestedAction {

    private final Action nestedAction;
    private List<Result> allResults;

    public ThroughputAction(Action nestedAction) {
        this.nestedAction = nestedAction;
    }

    @Override
    public void execute() throws Throwable {
        this.allResults = new ArrayList<>();

        StopWatch watch = new StopWatch();
        watch.start();
        nestedAction.execute();
        watch.stop();

        long timeInSeconds = watch.getTimeDiff() * 1000;

        List<Result> results = this.nestedAction.fetchResults();
        String filePath = getFilePath(results);
        double fileSizeInMB = getFileSizeInKB(filePath);
        double throughput = fileSizeInMB / timeInSeconds;

        results.add(new Result(ActionName.THROUGHPUT.getName(), true, throughput, "kB/s"));
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

    private String getFilePath(List<Result> results) {
        for (Result result : results) {
            if (result.getName().equals("ProductPath")) {
                return (String) result.getValue();
            }
        }
        return null;
    }

    private double getFileSizeInKB(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IllegalArgumentException("The given path does not exist: " + filePath);
        }

        long sizeInBytes = file.isFile()
                ? file.length()
                : calculateFolderSize(file);

        return sizeInBytes / 1024.0;
    }

    private long calculateFolderSize(File folder) {
        long totalSize = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                } else if (file.isDirectory()) {
                    totalSize += calculateFolderSize(file);
                }
            }
        }
        return totalSize;
    }

    @Override
    public Action getNestedAction() {
        return this.nestedAction;
    }
}
