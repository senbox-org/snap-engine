package org.esa.snap.remote.execution.file.system;

import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.executors.ProcessExecutor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 13/3/2019.
 */
public class WindowsLocalMachineFileSystem implements ILocalMachineFileSystem {

    private static final Logger logger = Logger.getLogger(WindowsLocalMachineFileSystem.class.getName());

    public WindowsLocalMachineFileSystem() {
    }

    @Override
    public char getFileSeparator() {
        return '\\';
    }

    @Override
    public String normalizeFileSeparator(String path) {
        return normalizeWindowsPath(path);
    }

    @Override
    public boolean pathStartsWith(String path, String prefix) {
        return StringUtils.startsWithIgnoreCase(path, prefix);
    }

    @Override
    public String findPhysicalSharedFolderPath(String shareNameToFind, String localPassword) throws IOException {
        String command = "net share " + shareNameToFind;
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeWindowsCommand(command, null, outputConsole);
        if (exitStatus == 0) {
            String[] normalOutput = outputConsole.getNormalStreamMessages().split("\n");
            String key = "Path ";
            String pathLine = null;
            for (int i = 0; i < normalOutput.length && pathLine == null; i++) {
                if (normalOutput[i].startsWith(key)) {
                    return normalOutput[i].substring(key.length()).trim();
                }
            }
        } else {
            // failed to get the physical shared folder path
            logger.log(Level.SEVERE, CommandExecutorUtils.buildCommandExecutedLogMessage("Failed to get the physical shared folder path of share name '"+shareNameToFind+"'.", command, outputConsole, exitStatus));
        }

        return null;
    }

    public static String normalizeWindowsPath(String path) {
        return path.replace('/', '\\');
    }
}
