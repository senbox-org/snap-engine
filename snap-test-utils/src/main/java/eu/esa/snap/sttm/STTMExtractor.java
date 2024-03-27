package eu.esa.snap.sttm;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.esa.snap.core.util.StringUtils;

public class STTMExtractor {

    public static final String DEFAULT_JIRA_URL = "https://senbox.atlassian.net/jira/software/c/projects/SNAP/issues/";

    public static void main(String[] args) throws IOException {
        final CmdLineArgs cmdLineArgs = validateInput(args);

        final ArrayList<STTMInfo> sttmInfos = new ArrayList<>();

        final STTMFileVisitor visitor = new STTMFileVisitor(sttmInfos);
        for (final String arg : cmdLineArgs.inputPaths) {
            Files.walkFileTree(Paths.get(arg), visitor);
        }

        final String jiraUrl = getJiraBaseUrl(cmdLineArgs);
        for (final STTMInfo sttmInfo : sttmInfos) {
            sttmInfo.jiraUrl = jiraUrl.concat(sttmInfo.jiraIssue);
        }

        final STTMExporter sttmExporter = new STTMExporter();
        final Path outPath = getPathAsserted(cmdLineArgs.outputPath);
        sttmExporter.writeTo(outPath, sttmInfos);
    }

    static String getJiraBaseUrl(CmdLineArgs args) {
        if (StringUtils.isNotNullAndNotEmpty(args.jiraBaseUrl)) {
            return args.jiraBaseUrl;
        }
        return DEFAULT_JIRA_URL;
    }

    static CmdLineArgs validateInput(String[] args) {
        if (args.length < 3) {
            printUsageTo(System.out);
            throw new IllegalArgumentException("Must provide at least one project path.");
        }

        final CmdLineArgs cmdLineArgs = new CmdLineArgs();
        final ArrayList<String> inputPaths = new ArrayList<>();

        boolean readOutput = false;
        boolean readUrl = false;
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

            if (arg.equals("-u")) {
                readUrl = true;
                continue;
            }
            if (readUrl) {
                cmdLineArgs.jiraBaseUrl = arg;
                readUrl = false;
                continue;
            }

            final Path projectDir = getPathAsserted(arg);
            if (!Files.exists(projectDir)) {
                throw new IllegalArgumentException("Directory does not exist: " + projectDir);
            }
            if (!Files.isDirectory(projectDir)) {
                throw new IllegalArgumentException("Input is not a directory: " + projectDir);
            }

            inputPaths.add(projectDir.toString());
        }

        final Path outDir = getPathAsserted(cmdLineArgs.outputPath);
        if (!Files.exists(outDir)) {
            throw new IllegalArgumentException("Directory does not exist: " + outDir);
        }

        cmdLineArgs.inputPaths = inputPaths.toArray(new String[0]);
        return cmdLineArgs;
    }

    private static Path getPathAsserted(String arg) {
        final Path projectDir;
        try {
            projectDir = Paths.get(arg);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return projectDir;
    }

    static void printUsageTo(OutputStream outputStream) {
        final PrintWriter writer = new PrintWriter(outputStream);

        writer.println("STTMExtractor usage:");
        writer.println();
        writer.println("-o <outdir>       Defines the output directory to write the report to.");
        writer.println("-u <jiraUrl>      Defines base url for Jira ticket reference (optional).");
        writer.println("<path> ... <path> Defines code paths to be parsed as blank separated list.");

        writer.flush();
    }
}
