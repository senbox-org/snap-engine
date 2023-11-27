package eu.esa.snap.sttm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class STTMExtractor {

    public static void main(String[] args) throws IOException {
        final CmdLineArgs cmdLineArgs = validateInput(args);

        final ArrayList<STTMInfo> sttmInfos = new ArrayList<>();

        final STTMFileVisitor visitor = new STTMFileVisitor(sttmInfos);
        for (final String arg : cmdLineArgs.inputPaths) {
            Files.walkFileTree(Paths.get(arg), visitor);
        }

        // @todo 3 allow to overwrite from cmd-line tb 2023-11-24
        final String jiraUrl = "https://senbox.atlassian.net/jira/software/c/projects/SNAP/issues/";
        for (final STTMInfo sttmInfo : sttmInfos) {
            sttmInfo.jiraUrl = jiraUrl.concat(sttmInfo.jiraIssue);
        }

        final STTMExporter sttmExporter = new STTMExporter();
        final Path outPath = Paths.get(cmdLineArgs.outputPath);
        sttmExporter.writeTo(outPath, sttmInfos);
    }

    static CmdLineArgs validateInput(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must provide at least one project path.");
        }

        final CmdLineArgs cmdLineArgs = new CmdLineArgs();
        final ArrayList<String> inputPaths = new ArrayList<>();

        boolean readOutput = false;
        for (final String arg : args) {
            if (arg.equals("-o")) {
                readOutput = true;
                continue;
            }
            if (readOutput) {
                cmdLineArgs.outputPath = arg;
                readOutput = false;
                continue;
            }

            final Path projectDir = Paths.get(arg);
            if (!Files.exists(projectDir)) {
                throw new IllegalArgumentException("Directory does not exist: " + projectDir);
            }
            if (!Files.isDirectory(projectDir)) {
                throw new IllegalArgumentException("Input is not a directory: " + projectDir);
            }

            inputPaths.add(projectDir.toString());
        }

        // @todo 1 tb/tb check for output path 2023-11-27

        cmdLineArgs.inputPaths = inputPaths.toArray(new String[0]);
        return cmdLineArgs;
    }
}
