package org.esa.snap.remote.execution.machines.executors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.executors.OutputConsole;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.core.gpf.graph.GraphException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Created by jcoravu on 22/1/2019.
 */
public class WindowsRemoteMachineExecutor extends AbstractRemoteMachineExecutor {

    public WindowsRemoteMachineExecutor(String masterSharedFolderPath, String sharedFolderUsername, String sharedFolderPassword,
                                        RemoteMachineProperties serverCredentials, RemoteMachinesGraphHelper remoteMachinesGraphHelper) {

        super(masterSharedFolderPath, sharedFolderUsername, sharedFolderPassword, serverCredentials, remoteMachinesGraphHelper);
    }

    @Override
    public String normalizeFileSeparator(String path) {
        return path.replace('/', '\\');
    }

    @Override
    public char getFileSeparatorChar() {
        return '\\';
    }

    @Override
    protected boolean runGraph(String gptFilePath, String graphFilePathToProcess) throws IOException, JSchException {
        String command = gptFilePath + " " + graphFilePathToProcess;
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeWindowsCommand(command, consoleBuffer);

        if (exitStatus == 0) {
            // the graph has been successfully executed on the remote machine
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, buildSuccessfullyExecutedGraphLogMessage(graphFilePathToProcess, command, consoleBuffer, exitStatus));
            }
            return true;
        }

        // failed to execute the graph on the remote machine
        logger.log(Level.SEVERE, buildFailedExecutedGraphLogMessage(graphFilePathToProcess, command, consoleBuffer, exitStatus));

        return false;
    }

    @Override
    protected boolean runCommands() throws IOException, GraphException, SftpException, JSchException {
        boolean mountSharedDrive = false;
        // mount the shared drive
        if (mountSharedDrive(this.remoteMachineCredentials.getSharedFolderPath())) {
            // the shared drive has been mounted
            mountSharedDrive = true;

            if (canContinueRunning()) {
                runGraphs();
            }
            // unmount the shared drive
            unmountSharedDrive(this.remoteMachineCredentials.getSharedFolderPath());
        }
        return mountSharedDrive;
    }

    private boolean unmountSharedDrive(String sharedDrive) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildWindowsUnmountSharedDriveCommand(sharedDrive);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeWindowsCommand(command, consoleBuffer);

        if (exitStatus == 0) {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully unmounted the shared drive '")
                        .append(sharedDrive)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }

            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Failed to unmount the shared drive '")
                .append(sharedDrive)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }

    private boolean mountSharedDrive(String sharedDrive) throws IOException, GraphException, SftpException, JSchException {
        String command = CommandExecutorUtils.buildWindowsMountSharedDriveCommand(this.masterSharedFolderPath, this.masterSharedFolderUsername, this.masterSharedFolderPassword, sharedDrive);
        OutputConsole consoleBuffer = new OutputConsole();
        int exitStatus = this.sshConnection.executeWindowsCommand(command, consoleBuffer);

        // do not write the password in the log file
        command = CommandExecutorUtils.buildWindowsMountSharedDriveCommand(masterSharedFolderPath, masterSharedFolderUsername, "...", sharedDrive);

        if (exitStatus == 0) {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder message = new StringBuilder();
                message.append("Successfully mounted the shared drive '")
                        .append(sharedDrive)
                        .append("' on the remote machine '")
                        .append(this.remoteMachineCredentials.getHostName())
                        .append("'.");
                logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));
            }

            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Failed to mount the shared drive '")
                .append(sharedDrive)
                .append("' on the remote machine '")
                .append(this.remoteMachineCredentials.getHostName())
                .append("'.");
        logger.log(Level.FINE, CommandExecutorUtils.buildCommandExecutedLogMessage(message.toString(), command, consoleBuffer, exitStatus));

        return false;
    }
}
