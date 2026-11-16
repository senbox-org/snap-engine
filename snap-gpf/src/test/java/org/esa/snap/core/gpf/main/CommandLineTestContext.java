package org.esa.snap.core.gpf.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Norman Fomferra
 */
public class CommandLineTestContext extends DefaultCommandLineContext {

    final StringBuffer printBuffer = new StringBuffer();
    final Map<String, String> textFiles = new HashMap<>();
    final List<StringReader> readers = new ArrayList<>();
    final Map<String, StringWriter> writers = new HashMap<>();

    @Override
    public Reader createReader(String filePath) throws IOException {
        final Path file = Path.of(filePath);
        final String fileName = file.getFileName().toString();
        if (!textFiles.containsKey(fileName)) {
            if (Files.exists(file)) {
                return Files.newBufferedReader(file);
            } else {
                throw new FileNotFoundException(filePath);
            }
        }
        StringReader stringReader = new StringReader(textFiles.get(fileName));
        readers.add(stringReader);
        return stringReader;
    }

    @Override
    public Writer createWriter(String fileName) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writers.put(fileName, stringWriter);
        return stringWriter;
    }

    @Override
    public boolean fileExists(String fileName) {
        return textFiles.containsKey(fileName);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger("TestCommandLineContext");
    }

    @Override
    public void print(String m) {
        printBuffer.append(m);
        printBuffer.append('\n');
    }

    @Override
    public String[] list(String path) throws IOException {
        Set<String> strings = textFiles.keySet();
        return strings.toArray(new String[strings.size()]);
    }

    @Override
    public boolean isFile(String path) {
        return true;
    }
}
