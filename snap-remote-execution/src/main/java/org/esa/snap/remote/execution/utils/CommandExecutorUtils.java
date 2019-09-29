package org.esa.snap.remote.execution.utils;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.executors.ProcessExecutor;
import org.esa.snap.remote.execution.executors.SSHConnection;
import org.esa.snap.remote.execution.file.system.UnixLocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.WindowsLocalMachineFileSystem;
import org.esa.snap.remote.execution.machines.ITestRemoteMachineConnection;
import org.esa.snap.remote.execution.machines.RemoteMachineConnectionResult;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 24/5/2019.
 */
public class CommandExecutorUtils {

    private static final Logger logger = Logger.getLogger(CommandExecutorUtils.class.getName());

    private CommandExecutorUtils() {
    }

    public static boolean mountWindowsLocalDrive(String masterSharedFolderURL, String masterSharedFolderUsername, String masterSharedFolderPassword, String localDrive)
                                                 throws IOException {

        String command = buildWindowsMountSharedDriveCommand(masterSharedFolderURL, masterSharedFolderUsername, masterSharedFolderPassword, localDrive);
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeWindowsCommand(command, null, outputConsole);

        boolean sharedDriveMounted = (exitStatus == 0);
        if (sharedDriveMounted) {
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully mounted the Windows shared drive '"+localDrive+"'.", command, outputConsole, exitStatus));
            }
        } else {
            logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to mount the Windows shared drive '"+localDrive+"'.", command, outputConsole, exitStatus));
        }

        return sharedDriveMounted;
    }

    public static boolean unmountWindowsLocalDrive(String localDrive) throws IOException {
        String command = buildWindowsUnmountSharedDriveCommand(localDrive);
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeWindowsCommand(command, null, outputConsole);

        boolean sharedDriveUnmounted = (exitStatus == 0);
        if (sharedDriveUnmounted) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully unmounted the Windows shared drive '"+localDrive+"'.", command, outputConsole, exitStatus));
            }
        } else {
            logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to unmount the Windows shared drive '"+localDrive+"'.", command, outputConsole, exitStatus));
        }

        return sharedDriveUnmounted;
    }

    public static UnixMountLocalFolderResult mountMacLocalFolder(String remoteSharedFolderURL, String remoteUsername, String remotePassword, String localSharedFolderPath)
                                                                 throws IOException {

        String normalizedLocalSharedFolderPath = UnixLocalMachineFileSystem.normalizeUnixPath(localSharedFolderPath);

        UnixMountLocalFolderResult localMachineMountFolder = new UnixMountLocalFolderResult();
        boolean localSharedFolderExists;
        if (Files.exists(Paths.get(normalizedLocalSharedFolderPath))) {
            // the local shared folder already exists on the local disk
            localSharedFolderExists = true;
        } else {
            // the local shared folder does not exist on the local disk
            localSharedFolderExists = makeUnixFolder(normalizedLocalSharedFolderPath, null);
            localMachineMountFolder.setSharedFolderCreated(localSharedFolderExists);
        }

        if (localSharedFolderExists) {
            // the changed mode command has been successfully executed on the local shared folder
            String normalizedRemoteSharedFolderURL = UnixLocalMachineFileSystem.normalizeUnixPath(remoteSharedFolderURL);

            String command = buildMacMountSharedFolderCommand(normalizedRemoteSharedFolderURL, normalizedLocalSharedFolderPath, remoteUsername, remotePassword);
            OutputConsole outputConsole = new OutputConsole();
            int exitStatus = ProcessExecutor.executeUnixCommand(command, null, null, outputConsole);

            command = buildMacMountSharedFolderCommand(normalizedRemoteSharedFolderURL, normalizedLocalSharedFolderPath, remoteUsername, "...");

            if (exitStatus == 0) {
                // the local shared folder has been successfully mounted
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully mount the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
                }

                localMachineMountFolder.setSharedFolderMounted(true);
            } else {
                // failed to mount the local shared folder
                logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to mount the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));

                if (localMachineMountFolder.isSharedFolderCreated()) {
                    // the local shared folder has not been mounted and remote the folder
                    boolean folderDeleted = deleteUnixLocalSharedFolder(normalizedLocalSharedFolderPath, null);
                    localMachineMountFolder.setSharedFolderCreated(!folderDeleted);
                }
            }
        }
        return localMachineMountFolder;
    }

    private static boolean makeUnixFolder(String normalizedLocalSharedFolderPath, String localPassword) throws IOException {
        String command = buildUnixMakeFolderCommand(normalizedLocalSharedFolderPath);
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);

        boolean localSharedFolderCreated = (exitStatus == 0);
        if (localSharedFolderCreated) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully created the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
            }
        } else {
            logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to create the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
        }

        return localSharedFolderCreated;
    }

    public static UnixMountLocalFolderResult mountLinuxLocalFolder(String remoteSharedFolderURL, String remoteUsername, String remotePassword,
                                                                   String localSharedFolderPath, String localPassword)
                                                                   throws IOException {

        String normalizedLocalSharedFolderPath = UnixLocalMachineFileSystem.normalizeUnixPath(localSharedFolderPath);

        UnixMountLocalFolderResult localMachineMountFolder = new UnixMountLocalFolderResult();
        boolean localSharedFolderExists;
        if (Files.exists(Paths.get(normalizedLocalSharedFolderPath))) {
            // the local shared folder already exists on the local disk
            localSharedFolderExists = true;
        } else {
            // the local shared folder does not exist on the local disk
            localSharedFolderExists = makeUnixFolder(normalizedLocalSharedFolderPath, localPassword);
            localMachineMountFolder.setSharedFolderCreated(localSharedFolderExists);
        }

        if (localSharedFolderExists) {
            String command = buildLinuxChangeModeFolderCommand(normalizedLocalSharedFolderPath);
            OutputConsole outputConsole = new OutputConsole();
            int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);

            if (exitStatus == 0) {
                // the changed mode command has been successfully executed on the local shared folder
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully change mode of the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
                }

                String normalizedRemoteSharedFolderURL = UnixLocalMachineFileSystem.normalizeUnixPath(remoteSharedFolderURL);

                command = buildLinuxMountSharedFolderCommand(normalizedRemoteSharedFolderURL, normalizedLocalSharedFolderPath, remoteUsername, remotePassword);
                outputConsole = new OutputConsole();
                exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);

                command = buildLinuxMountSharedFolderCommand(normalizedRemoteSharedFolderURL, normalizedLocalSharedFolderPath, remoteUsername, "...");

                if (exitStatus == 0) {
                    // the local shared folder has been successfully mounted
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully mount the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
                    }

                    localMachineMountFolder.setSharedFolderMounted(true);
                } else {
                    // failed to mount the local shared folder
                    logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to mount the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));

                    if (localMachineMountFolder.isSharedFolderCreated()) {
                        // the local shared folder has not been mounted and remote the folder
                        boolean folderDeleted = deleteUnixLocalSharedFolder(normalizedLocalSharedFolderPath, localPassword);
                        localMachineMountFolder.setSharedFolderCreated(!folderDeleted);
                    }
                }
            } else {
                // failed to change mode
                logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to change mode of the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
            }
        }
        return localMachineMountFolder;
    }

    private static boolean unmountUnixLocalSharedFolder(String localSharedFolderPath, String localPassword, UnixMountLocalFolderResult unixMountLocalFolderResult, boolean isLinux)
                                                        throws IOException {

        String normalizedLocalSharedFolderPath = UnixLocalMachineFileSystem.normalizeUnixPath(localSharedFolderPath);

        String command;
        if (isLinux) {
            command = buildUnmountUnixSharedFolderCommand(normalizedLocalSharedFolderPath);
        } else {
            command = "diskutil umount force " + normalizedLocalSharedFolderPath;
        }
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);

        boolean localSharedFolderUnmounted = (exitStatus == 0);
        if (localSharedFolderUnmounted) {
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully unmounted the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
            }
        } else {
            logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to unmount the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
        }

        if (unixMountLocalFolderResult.isSharedFolderCreated()) {
            deleteUnixLocalSharedFolder(normalizedLocalSharedFolderPath, localPassword);
        }
        return localSharedFolderUnmounted;
    }

    public static void unmountLinuxLocalFolder(String localSharedFolderPath, String localPassword, UnixMountLocalFolderResult unixMountLocalFolderResult) throws IOException {
        unmountUnixLocalSharedFolder(localSharedFolderPath, localPassword, unixMountLocalFolderResult, true);
    }

    public static void unmountMacLocalFolder(String localSharedFolderPath, UnixMountLocalFolderResult unixMountLocalFolderResult) throws IOException {
        unmountUnixLocalSharedFolder(localSharedFolderPath, null, unixMountLocalFolderResult, false); // 'null' => no password for Mac
    }

    private static boolean deleteUnixLocalSharedFolder(String normalizedLocalSharedFolderPath, String localPassword) throws IOException {
        String command = buildUnixRemoveFolderCommand(normalizedLocalSharedFolderPath);
        OutputConsole outputConsole = new OutputConsole();
        int exitStatus = ProcessExecutor.executeUnixCommand(command, localPassword, null, outputConsole);

        boolean localSharedFolderDeleted = (exitStatus == 0);
        if (localSharedFolderDeleted) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, buildCommandExecutedLogMessage("Successfully deleted the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
            }
        } else {
            logger.log(Level.SEVERE, buildCommandExecutedLogMessage("Failed to delete the local shared folder '"+normalizedLocalSharedFolderPath+"'.", command, outputConsole, exitStatus));
        }

        return localSharedFolderDeleted;
    }

    public static String buildCommandExecutedLogMessage(String message, String command, OutputConsole consoleBuffer, int exitStatus) {
        String newLineAndTab = "\n\t";
        String normalKey = "Normal: ";
        String errorKey = "Error: ";

        String emptySpaces = buildStringWithEmptySpaces(normalKey.length());
        String normalMessages = consoleBuffer.getNormalStreamMessages().replace(OutputConsole.MESSAGE_SEPARATOR, newLineAndTab + emptySpaces);

        emptySpaces = buildStringWithEmptySpaces(errorKey.length());
        String errorMessages = consoleBuffer.getErrorStreamMessages().replace(OutputConsole.MESSAGE_SEPARATOR, newLineAndTab + emptySpaces);

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(message)
                .append(newLineAndTab)
                .append("Command: ")
                .append(command)
                .append(newLineAndTab)
                .append("Exit status: ")
                .append(exitStatus)
                .append(newLineAndTab)
                .append(normalKey)
                .append(normalMessages)
                .append(newLineAndTab)
                .append(errorKey)
                .append(errorMessages);
        return logMessage.toString();
    }

    public static String buildUnmountUnixSharedFolderCommand(String mountedFolderPath) {
        return "umount " + mountedFolderPath;
    }

    public static String buildUnixRemoveFolderCommand(String folderPath) {
        return "rmdir " + folderPath;
    }

    public static String buildUnixMakeFolderCommand(String folderPath) {
        return "mkdir " + folderPath;
    }

    public static String buildLinuxChangeModeFolderCommand(String folderPath) {
        return "chmod -R 0777 "+ folderPath;
    }

    public static String buildMacMountSharedFolderCommand(String remoteMachineSharedFolderURL, String localFolderPathToMount,
                                                          String remoteMachineUsername, String remoteMachinePassword) {

        StringBuilder command = new StringBuilder();
        command.append("mount -t smbfs")
                .append(" //")
                .append(remoteMachineUsername)
                .append(":")
                .append(remoteMachinePassword)
                .append("@")
                .append(remoteMachineSharedFolderURL.substring(2))
                .append(" ")
                .append(localFolderPathToMount);
        return command.toString();
    }

    public static String buildLinuxMountSharedFolderCommand(String remoteMachineSharedFolderURL, String localFolderPathToMount,
                                                            String remoteMachineUsername, String remoteMachinePassword) {

        StringBuilder command = new StringBuilder();
        command.append("mount.cifs")
                .append(" ")
                .append(remoteMachineSharedFolderURL)
                .append(" ")
                .append(localFolderPathToMount)
                .append(" ")
                .append("-o user=")
                .append(remoteMachineUsername)
                .append(",password=")
                .append(remoteMachinePassword)
                .append(",file_mode=0777,dir_mode=0777,noperm");
        return command.toString();
    }

    public static String buildWindowsMountSharedDriveCommand(String masterSharedFolderURL, String masterSharedFolderUsername,
                                                             String masterSharedFolderPassword, String localDrive) {

        StringBuilder command = new StringBuilder();
        command.append("net use")
                .append(" ")
                .append(localDrive)
                .append(" ")
                .append(WindowsLocalMachineFileSystem.normalizeWindowsPath(masterSharedFolderURL))
                .append(" ")
                .append(masterSharedFolderPassword)
                .append(" ")
                .append("/user:")
                .append(masterSharedFolderUsername)
                .append(" ")
                .append("/persistent:no");
        return command.toString();
    }

    public static String buildWindowsUnmountSharedDriveCommand(String localDrive) {
        StringBuilder command = new StringBuilder();
        command.append("net use")
                .append(" ")
                .append(localDrive)
                .append(" ")
                .append("/delete /y");
        return command.toString();
    }

    public static RemoteMachineConnectionResult canConnectToRemoteMachine(RemoteMachineProperties machineCredentials, ITestRemoteMachineConnection callback)
            throws JSchException, IOException {

        // check if can connect to the remote machine
        callback.testSSHConnection();

        SSHConnection sshConnection = new SSHConnection(machineCredentials.getHostName(), machineCredentials.getPortNumber(), machineCredentials.getUsername(), machineCredentials.getPassword());
        sshConnection.connect();
        try {
            // the remote machine is available
            boolean isRemoteMachineAvailable = true;

            // check if the GPT application is available on the remote machine
            callback.testGPTApplication();

            String gptFilePath;
            if (StringUtils.isBlank(machineCredentials.getGPTFilePath())) {
                gptFilePath = "gpt";
            } else {
                gptFilePath = machineCredentials.getGPTFilePath();
            }
            String command = gptFilePath;
            OutputConsole consoleBuffer = new OutputConsole();
            int exitStatus = sshConnection.executeWindowsCommand(command, consoleBuffer);
            boolean isGPTApplicationAvailable;
            if (exitStatus == 0) {
                // the GPT application has been successfully tested on the remote machine
                isGPTApplicationAvailable = true;
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, buildSuccessfullyTestedGPTApplicationLogMessage(machineCredentials.getHostName(), gptFilePath, command, consoleBuffer, exitStatus));
                }
            } else {
                // failed to test the GPT application on the remote machine
                isGPTApplicationAvailable = false;
                logger.log(Level.SEVERE, buildFailedTestedGPTApplicationLogMessage(machineCredentials.getHostName(), gptFilePath, command, consoleBuffer, exitStatus));
            }

            return new RemoteMachineConnectionResult(isRemoteMachineAvailable, isGPTApplicationAvailable);
        } finally {
            sshConnection.disconnect();
        }
    }

    private static String buildSuccessfullyTestedGPTApplicationLogMessage(String remoteMachineHost, String gptFilePath, String command, OutputConsole consoleBuffer, int exitStatus) {
        StringBuilder message = new StringBuilder();
        message.append("Successfully tested the GPT application '")
                .append(gptFilePath)
                .append("' on the remote machine '")
                .append(remoteMachineHost)
                .append("'.");
        return CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus);
    }

    private static String buildFailedTestedGPTApplicationLogMessage(String remoteMachineHost, String gptFilePath, String command, OutputConsole consoleBuffer, int exitStatus) {
        StringBuilder message = new StringBuilder();
        message.append("Failed to test the GPT application '")
                .append(gptFilePath)
                .append("' on the remote machine '")
                .append(remoteMachineHost)
                .append("'.");
        return CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus);
    }

    private static String buildStringWithEmptySpaces(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i=0; i<length; i++) {
            result.append(' ');
        }
        return result.toString();
    }
}
