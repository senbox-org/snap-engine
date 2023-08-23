package eu.esa.snap.sttm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class STTMFileVisitor extends SimpleFileVisitor<Path> {

    private static final String REG_EX = "\\S*Test\\S*.java";

    private final List<STTMInfo> sttmInfos;

    STTMFileVisitor(List<STTMInfo> sttmInfos) {
        this.sttmInfos = sttmInfos;
    }

    static List<STTMInfo> extractSTTMInfo(BufferedReader reader) throws IOException {
        final ArrayList<STTMInfo> resultList = new ArrayList<>();
        String line;
        String pckg = null;
        String clazz = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim(); // just to be sure to have no leading or trailing whitespace tb 2023-08-22

            if (line.startsWith("package")) {
                pckg = extractPackage(line);
            }

            if (line.contains(" class ")) {
                clazz = extractClass(line);
            }

            if (line.startsWith("@STTM(")) {
                // @todo 1 implement code to extract the STTM info here  tb 2023-08-23
                final String[] jiraIssues = extractJiraIssues(line);
                for (final String issue: jiraIssues) {
                    final STTMInfo sttmInfo = new STTMInfo();
                    sttmInfo.jiraIssue = issue;
                    sttmInfo.pckg = pckg;
                    sttmInfo.clazz = clazz;
                    resultList.add(sttmInfo);
                }
            }
        }

        return resultList;
    }

    static String extractClass(String line) {
        line = line.replaceAll(" +", " "); // ensure that we only have single blanks as separator tb 2023-08-23
        final StringTokenizer stringTokenizer = new StringTokenizer(line, " ", false);

        boolean nextTokenIsClassName = false;
        while (stringTokenizer.hasMoreTokens()) {
            final String token = stringTokenizer.nextToken();

            if (nextTokenIsClassName) {
                return token;
            }

            if (token.equals("class")) {
                nextTokenIsClassName = true;
            }
        }
        return null;
    }

    static String extractPackage(String line) {
        int startIdx = 7;
        int stopIdx = line.indexOf(";", startIdx + 1);
        return line.substring(startIdx + 1, stopIdx).trim();
    }

    static String[] extractJiraIssues(String line) {
        int startIdx = line.indexOf("\"");
        int stopIdx = line.indexOf("\"", startIdx + 1);

        final String substring = line.substring(startIdx + 1, stopIdx);
        final StringTokenizer stringTokenizer = new StringTokenizer(substring, ",", false);
        final ArrayList<String> issueList = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            issueList.add(stringTokenizer.nextToken().trim());
        }

        return issueList.toArray(new String[0]);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final String fileName = file.getFileName().toString();
        if (!fileName.matches(REG_EX)) {
            return FileVisitResult.CONTINUE;
        }

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            final List<STTMInfo> fileSTTMInfos = extractSTTMInfo(reader);
            if (fileSTTMInfos.size() > 0) {
                sttmInfos.addAll(fileSTTMInfos);
            }
        }

        return FileVisitResult.CONTINUE;
    }
}
