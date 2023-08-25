package eu.esa.snap.sttm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class STTMExtractor {

    public static void main(String[] args) throws IOException {
        validateInput(args);

        final ArrayList<STTMInfo> sttmInfos = new ArrayList<>();

        final STTMFileVisitor visitor = new STTMFileVisitor(sttmInfos);
        for (final String arg : args) {
            Files.walkFileTree(Paths.get(arg), visitor);
        }

        for (final STTMInfo sttmInfo : sttmInfos) {
            System.out.println("issue:   " + sttmInfo.jiraIssue);
            System.out.println("package: " + sttmInfo.pckg);
            System.out.println("class:   " + sttmInfo.clazz);
            System.out.println("method:  " + sttmInfo.method);
            System.out.println("------------------");
        }
    }

    static void validateInput(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must provide at least one project path.");
        }

        for (final String arg : args) {
            final Path projectDir = Paths.get(arg);
            if (!Files.exists(projectDir)) {
                throw new IllegalArgumentException("Directory does not exist: " + projectDir);
            }
            if (!Files.isDirectory(projectDir)) {
                throw new IllegalArgumentException("Input is not a directory: " + projectDir);
            }
        }
    }
}
