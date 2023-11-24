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

        // @todo 3 allow to overwrite from cmd-line tb 2023-11-24
        final String jiraUrl = "https://senbox.atlassian.net/jira/software/c/projects/SNAP/issues/";
        for (final STTMInfo sttmInfo : sttmInfos) {
            sttmInfo.jiraUrl = jiraUrl.concat(sttmInfo.jiraIssue);
        }


        // @todo 1 sort by (issue/module/...) tb 2023-11-23

        final STTMExporter sttmExporter = new STTMExporter();
        // @todo 1 tb/tb read from cmd-line 2023-11-23
        final Path outPath = Paths.get("C:/Satellite/DELETE");
        sttmExporter.writeTo(outPath, sttmInfos);

        /*
        for (final STTMInfo sttmInfo : sttmInfos) {
            System.out.println("issue:   " + sttmInfo.jiraIssue);
            System.out.println("package: " + sttmInfo.pckg);
            System.out.println("class:   " + sttmInfo.clazz);
            System.out.println("method:  " + sttmInfo.method);
            System.out.println("module:  " + sttmInfo.module);
            System.out.println("------------------");
        }
        */
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
