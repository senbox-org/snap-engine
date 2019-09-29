package org.esa.snap.remote.execution;

import com.bc.ceres.binding.converters.BooleanConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.StringConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.PrintWriterConciseProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.esa.snap.remote.execution.converters.RemoteMachinePropertiesConverter;
import org.esa.snap.remote.execution.converters.SourceProductFilesConverter;
import org.esa.snap.remote.execution.exceptions.ExecutionExceptionType;
import org.esa.snap.remote.execution.exceptions.OperatorExecutionException;
import org.esa.snap.remote.execution.exceptions.OperatorInitializeException;
import org.esa.snap.remote.execution.exceptions.WaitingTimeoutException;
import org.esa.snap.remote.execution.file.system.ILocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.LinuxLocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.MacLocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.UnixLocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.WindowsLocalMachineFileSystem;
import org.esa.snap.remote.execution.machines.executors.AbstractRemoteMachineExecutor;
import org.esa.snap.remote.execution.machines.executors.LinuxRemoteMachineExecutor;
import org.esa.snap.remote.execution.machines.executors.RemoteMachineExecutorResult;
import org.esa.snap.remote.execution.machines.executors.RemoteMachinesGraphHelper;
import org.esa.snap.remote.execution.machines.executors.SlaveProductsInputData;
import org.esa.snap.remote.execution.machines.executors.WindowsRemoteMachineExecutor;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.remote.execution.utils.CommandExecutorUtils;
import org.esa.snap.remote.execution.utils.ThreadNamePoolExecutor;
import org.esa.snap.remote.execution.utils.UnixMountLocalFolderResult;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.xmlpull.mxp1.MXParser;

import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/12/2018.
 */
@OperatorMetadata(
        alias = "RemoteExecutionOp",
        version="1.0",
        category = "Menu/Tools",
        autoWriteDisabled = true,
        description = "The Remote Execution Processor executes on the remote machines a slave graph and then on the host machine it executes a master graph using the products created by the remote machines.",
        authors = "Jean Coravu",
        copyright = "Copyright (C) 2018 by CS ROMANIA")
public class RemoteExecutionOp extends Operator {

    private static final Logger logger = Logger.getLogger(RemoteExecutionOp.class.getName());

    @Parameter(itemAlias = "sharedFolderPath", notNull = true, converter = StringConverter.class, description = "Specifies the shared folder path.")
    private String remoteSharedFolderPath;

    @Parameter(itemAlias = "username", notNull = true, converter = StringConverter.class, description = "Specifies the username account of the machine where the shared folder is created.")
    private String remoteSharedFolderUsername;

    @Parameter(itemAlias = "password", notNull = true, converter = StringConverter.class, description = "Specifies the password account of the machine where the shared folder is created.")
    private String remoteSharedFolderPassword;

    @Parameter(itemAlias = "localSharedFolderPath", notNull = true, converter = StringConverter.class, description = "Specifies the local shared folder path used to connect to the remote shared folder.")
    private String localSharedFolderPath;

    @Parameter(itemAlias = "localPassword", notNull = false, converter = StringConverter.class, description = "Specifies the password of the local machine.")
    private String localPassword;

    @Parameter(itemAlias = "slaveGraphFilePath", notNull = true, converter = StringConverter.class, description = "Specifies the slave graph file path to be executed on the remote machines.")
    private String slaveGraphFilePath;

    @Parameter(itemAlias = "sourceFiles", notNull = true, converter = SourceProductFilesConverter.class, description = "Specifies the product files.")
    private String[] sourceProductFiles;

    @Parameter(itemAlias = "machines", notNull = true, converter = RemoteMachinePropertiesConverter.class, description = "Specifies the remote machines credentials.")
    private RemoteMachineProperties[] remoteMachines;

    @Parameter(itemAlias = "masterGraphFilePath", notNull = true, converter = StringConverter.class, description = "Specifies the master graph file path.")
    private String masterGraphFilePath;

    @Parameter(itemAlias = "continueOnFailure", notNull = true, converter = BooleanConverter.class, description = "Specifies the flag to continue or not when a remote machine fails.")
    private Boolean continueOnFailure;

    @Parameter(itemAlias = "masterProductFormatName", notNull = true, converter = StringConverter.class, description = "Specifies the master product format name.")
    private String masterProductFormatName;

    @Parameter(itemAlias = "masterProductFilePath", notNull = true, converter = StringConverter.class, description = "Specifies the master product file path.")
    private String masterProductFilePath;

    @Parameter(itemAlias = "waitingSecondsTimeout", notNull = false, converter = IntegerConverter.class, description = "Specifies the waiting seconds to complete the output products on the remote machines.")
    private int waitingSecondsTimeout;

    @Parameter(itemAlias = "slaveProductsFolderNamePrefix", notNull = false, converter = StringConverter.class, description = "Specifies the folder name prefix of the slave output products.")
    private String slaveProductsFolderNamePrefix;

    @Parameter(itemAlias = "slaveProductsName", notNull = false, converter = StringConverter.class, description = "Specifies the name of the output products obtained using the slave graph.")
    private String slaveProductsName;

    @Parameter(itemAlias = "slaveProductsFormatName", notNull = false, converter = StringConverter.class, description = "Specifies the format name of the output products obtained using the slave graph.")
    private String slaveProductsFormatName;

    @TargetProduct
    private Product targetProduct;

    public RemoteExecutionOp() {
        this.waitingSecondsTimeout = 0;
    }

    @Override
    public void initialize() throws OperatorException {
        if (StringUtils.isBlank(this.remoteSharedFolderPath)) {
            throw new OperatorInitializeException("The remote shared folder path is not specified.");
        }
        // check if the master shared folder path specifies a shared folder
        ILocalMachineFileSystem localMachineFileSystem;
        if (SystemUtils.IS_OS_MAC) {
            localMachineFileSystem = new MacLocalMachineFileSystem();
        } else if (SystemUtils.IS_OS_LINUX) {
            localMachineFileSystem = new LinuxLocalMachineFileSystem();
        } else if (SystemUtils.IS_OS_WINDOWS) {
            localMachineFileSystem = new WindowsLocalMachineFileSystem();
        } else {
            throw new OperatorInitializeException("Unsupported operating system '" + SystemUtils.OS_NAME + "'.");
        }
        String normalizedMasterSharedFolderPath = localMachineFileSystem.normalizeFileSeparator(this.remoteSharedFolderPath);
        String prefix = "" + localMachineFileSystem.getFileSeparator() + localMachineFileSystem.getFileSeparator();
        if (normalizedMasterSharedFolderPath.startsWith(prefix)) {
            int index = normalizedMasterSharedFolderPath.indexOf(localMachineFileSystem.getFileSeparator(), prefix.length() + 1);
            String masterHostName = normalizedMasterSharedFolderPath.substring(prefix.length(), index);
            if (isMasterHostIdenticalWithLocalHost(masterHostName)) {
                // the master host name is the same with the running host
                String physicalSharedFolderPath = null;
                try {
                    String shareName = normalizedMasterSharedFolderPath.substring(index + 1);
                    physicalSharedFolderPath = localMachineFileSystem.findPhysicalSharedFolderPath(shareName, this.localPassword);
                } catch (IOException exception) {
                    logger.log(Level.SEVERE, "Failed to process the local source products.", exception);
                }

                if (physicalSharedFolderPath != null) {
                    logger.log(Level.FINE, "The remote shared folder path '"+this.remoteSharedFolderPath +"' is mapped to the local machine folder path '"+ physicalSharedFolderPath+"'.");

                    String normalizedPhysicalSharedFolderPath = localMachineFileSystem.normalizeFileSeparator(physicalSharedFolderPath);
                    // add the file separator at the end
                    if (!normalizedPhysicalSharedFolderPath.endsWith("" + localMachineFileSystem.getFileSeparator())) {
                        normalizedPhysicalSharedFolderPath += localMachineFileSystem.getFileSeparator();
                    }
                    for (int i = 0; i < this.sourceProductFiles.length; i++) {
                        String normalizedSourceProductFilePath = localMachineFileSystem.normalizeFileSeparator(this.sourceProductFiles[i]);
                        if (localMachineFileSystem.pathStartsWith(normalizedSourceProductFilePath, normalizedPhysicalSharedFolderPath)) {
                            // rewrite the source product file path
                            String relativePath = normalizedSourceProductFilePath.substring(normalizedPhysicalSharedFolderPath.length());
                            this.sourceProductFiles[i] = this.remoteSharedFolderPath + localMachineFileSystem.getFileSeparator() + relativePath;
                        }
                    }
                }
            }
        } else {
            throw new OperatorInitializeException("The remote shared folder path '"+this.remoteSharedFolderPath +"' does not specifies a shared folder.");
        }

        if (StringUtils.isBlank(this.remoteSharedFolderUsername)) {
            throw new OperatorInitializeException("The username to access the remote shared folder path is not specified.");
        }
        if (StringUtils.isBlank(this.remoteSharedFolderPassword)) {
            throw new OperatorInitializeException("The password to access the remote shared folder path is not specified.");
        }
        if (this.remoteMachines == null || this.remoteMachines.length == 0) {
            throw new OperatorInitializeException("The remote machines are not specified.");
        }
        if (StringUtils.isBlank(this.slaveGraphFilePath)) {
            throw new OperatorInitializeException("The slave graph file path is not specified.");
        }
        if (this.continueOnFailure == null) {
            throw new OperatorInitializeException("The flag to continue or not when a remote machine fails is not specified.");
        }
        if (this.sourceProductFiles == null || this.sourceProductFiles.length == 0) {
            throw new OperatorInitializeException("The source product files are not specified.");
        }
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            if (StringUtils.isBlank(this.localSharedFolderPath)) {
                throw new OperatorInitializeException("The local shared folder path must be specified when running on Unix operating systems.");
            }
            Path path;
            try {
                String normalizedLocalSharedFolderPath = UnixLocalMachineFileSystem.normalizeUnixPath(this.localSharedFolderPath);
                path = buildPath(normalizedLocalSharedFolderPath);
            } catch (URISyntaxException exception) {
                throw new OperatorInitializeException("The local shared folder path '"+this.localSharedFolderPath+"' is incorrect.", exception);
            }
            if (!path.isAbsolute()) {
                throw new OperatorInitializeException("The local shared folder path '"+this.localSharedFolderPath+"' does not represent an absolute path on Unix operating systems.");
            }
            if (SystemUtils.IS_OS_LINUX) {
                if (StringUtils.isBlank(this.localPassword)) {
                    throw new OperatorInitializeException("The local password must be specified when running on Linux.");
                }
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (!StringUtils.isBlank(this.localSharedFolderPath)) {
                // the local shared drive is specified
                char driveLetter = this.localSharedFolderPath.charAt(0);
                if (!Character.isLetter(driveLetter)) {
                    throw new OperatorInitializeException("The local shared folder path does not represent a drive when running on Windows. The first character '"+driveLetter+"' is not a letter.");
                }
                String colon = this.localSharedFolderPath.substring(1);
                if (!":".equals(colon)) {
                    throw new OperatorInitializeException("Expected 'X:' for local shared driver when running on Windows.");
                }
            }
        }
        if (canCreateTargetProduct()) {
            if (StringUtils.isBlank(this.masterGraphFilePath)) {
                throw new OperatorInitializeException("The master graph file path is not specified.");
            }
            if (StringUtils.isBlank(this.masterProductFilePath)) {
                throw new OperatorInitializeException("The target product file path is not specified.");
            }
            if (StringUtils.isBlank(this.masterProductFormatName)) {
                throw new OperatorInitializeException("The target format name is not specified.");
            }
        }

        if (StringUtils.isBlank(this.slaveProductsName)) {
            this.slaveProductsName = "remote-product.dim";
        }

        // create a dummy target product required by the operator context
        int sceneRasterWidth = 1;
        int sceneRasterHeight = 1;
        this.targetProduct = new Product("CloudExploitationPlatform", "test", sceneRasterWidth, sceneRasterHeight);
        Band targetBand = new Band("band_1", ProductData.TYPE_INT32, sceneRasterWidth, sceneRasterHeight);
        this.targetProduct.addBand(targetBand);
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            // mount the local shared folder
            UnixMountLocalFolderResult unixMountLocalFolderResult = null;
            ILocalMachineFileSystem localMachineFileSystem;
            String localMachineSharedFolderPath;
            if (SystemUtils.IS_OS_LINUX) {
                unixMountLocalFolderResult = CommandExecutorUtils.mountLinuxLocalFolder(this.remoteSharedFolderPath, this.remoteSharedFolderUsername,
                                                                this.remoteSharedFolderPassword, this.localSharedFolderPath, this.localPassword);
                if (!unixMountLocalFolderResult.isSharedFolderMounted()) {
                    throw new OperatorException("Failed to mount the local shared folder '" + this.localSharedFolderPath + "' to the remote shared folder '" + this.remoteSharedFolderPath + "'.");
                }
                // the local shared folder has been successfully mounted
                localMachineFileSystem = new LinuxLocalMachineFileSystem();
                localMachineSharedFolderPath = this.localSharedFolderPath;
            } else if (SystemUtils.IS_OS_WINDOWS) {
                localMachineFileSystem = new WindowsLocalMachineFileSystem();
                if (StringUtils.isBlank(this.localSharedFolderPath)) {
                    // no local shared drive to mount on Windows
                    localMachineSharedFolderPath = this.remoteSharedFolderPath;
                } else {
                    boolean mounted = CommandExecutorUtils.mountWindowsLocalDrive(this.remoteSharedFolderPath, this.remoteSharedFolderUsername, this.remoteSharedFolderPassword, this.localSharedFolderPath);
                    if (!mounted) {
                        throw new OperatorException("Failed to mount the local drive '" + this.localSharedFolderPath + "' to the remote shared folder '" + this.remoteSharedFolderPath + "'.");
                    }
                    // the local shared folder has been successfully mounted
                    localMachineSharedFolderPath = this.localSharedFolderPath;
                }
            } else if (SystemUtils.IS_OS_MAC) {
                unixMountLocalFolderResult = CommandExecutorUtils.mountMacLocalFolder(this.remoteSharedFolderPath, this.remoteSharedFolderUsername,
                                                                    this.remoteSharedFolderPassword, this.localSharedFolderPath);
                if (!unixMountLocalFolderResult.isSharedFolderMounted()) {
                    throw new OperatorException("Failed to mount the local shared folder '" + this.localSharedFolderPath + "' to the remote shared folder '" + this.remoteSharedFolderPath + "'.");
                }
                // the local shared folder has been successfully mounted
                localMachineFileSystem = new MacLocalMachineFileSystem();
                localMachineSharedFolderPath = this.localSharedFolderPath;
            } else {
                throw new UnsupportedOperationException("Unsupported operating system '" + SystemUtils.OS_NAME + "'.");
            }
            try {
                String normalizedRemoteSharedFolderPath = localMachineFileSystem.normalizeFileSeparator(this.remoteSharedFolderPath + "/");
                String normalizedLocalSharedFolderPath = localMachineFileSystem.normalizeFileSeparator(localMachineSharedFolderPath + "/");

                Path localSharedFolder = buildPath(normalizedLocalSharedFolderPath);

                String[] relativeSourceProductFilePaths = new String[this.sourceProductFiles.length];
                for (int i = 0; i < this.sourceProductFiles.length; i++) {
                    String normalizedSourceProductFilePath = localMachineFileSystem.normalizeFileSeparator(this.sourceProductFiles[i]);
                    String relativePath;
                    if (localMachineFileSystem.pathStartsWith(normalizedSourceProductFilePath, normalizedRemoteSharedFolderPath)) {
                        // the source product path starts with the master shared folder path
                        relativePath = normalizedSourceProductFilePath.substring(normalizedRemoteSharedFolderPath.length());
                    } else if (localMachineFileSystem.pathStartsWith(normalizedSourceProductFilePath, normalizedLocalSharedFolderPath)) {
                        // the source product path starts with the local shared folder path
                        relativePath = normalizedSourceProductFilePath.substring(normalizedLocalSharedFolderPath.length());
                    } else {
                        // the source product path is invalid
                        StringBuilder errorMessage = new StringBuilder();
                        errorMessage.append("The source product file '")
                                .append(normalizedSourceProductFilePath)
                                .append("' does not start with the remote shared folder '")
                                .append(normalizedRemoteSharedFolderPath);
                        if (!normalizedRemoteSharedFolderPath.equalsIgnoreCase(normalizedLocalSharedFolderPath)) {
                            errorMessage.append("' neither the local shared folder '")
                                        .append(normalizedLocalSharedFolderPath);
                        }
                        errorMessage.append("'.");
                        throw new OperatorException(errorMessage.toString());
                    }
                    Path sourceFilePath = localSharedFolder.resolve(relativePath);
                    if (Files.exists(sourceFilePath)) {
                        relativeSourceProductFilePaths[i] = relativePath;
                    } else {
                        throw new OperatorException("The source product file '" + normalizedSourceProductFilePath + "' does not exist into the remote shared folder.");
                    }
                }

                Path slaveGraphFile = checkGraphFileIfExists(localMachineFileSystem, normalizedRemoteSharedFolderPath, localSharedFolder, this.slaveGraphFilePath, false);
                Path masterGraphFile = null;
                if (canCreateTargetProduct()) {
                    masterGraphFile = checkGraphFileIfExists(localMachineFileSystem, normalizedRemoteSharedFolderPath, localSharedFolder, this.masterGraphFilePath, true);
                }

                writeGraphsToSharedFolder(localMachineFileSystem, localSharedFolder, relativeSourceProductFilePaths, slaveGraphFile, masterGraphFile);
            } finally {
                // unmount the local shared folder
                if (SystemUtils.IS_OS_LINUX) {
                    CommandExecutorUtils.unmountLinuxLocalFolder(this.localSharedFolderPath, this.localPassword, unixMountLocalFolderResult);
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    if (!StringUtils.isBlank(this.localSharedFolderPath)) {
                        CommandExecutorUtils.unmountWindowsLocalDrive(this.localSharedFolderPath);
                    }
                } else if (SystemUtils.IS_OS_MAC) {
                    CommandExecutorUtils.unmountMacLocalFolder(this.localSharedFolderPath, unixMountLocalFolderResult);
                }
            }
        } catch (OperatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OperatorException("Failed to execute the operator.", exception);
        }
    }

    public boolean canCreateTargetProduct() {
        if (StringUtils.isBlank(this.masterGraphFilePath) && StringUtils.isBlank(this.masterProductFilePath) && StringUtils.isBlank(this.masterProductFormatName)) {
            return false;
        }
        return true;
    }

    private AbstractRemoteMachineExecutor buildRemoteMachineExecutor(RemoteMachineProperties remoteMachineCredentials, RemoteMachinesGraphHelper remoteMachinesGraphHelper) {
        if (remoteMachineCredentials.isLinux()) {
            return new LinuxRemoteMachineExecutor(this.remoteSharedFolderPath, this.remoteSharedFolderUsername, this.remoteSharedFolderPassword, remoteMachineCredentials, remoteMachinesGraphHelper);
        } else if (remoteMachineCredentials.isWindows()) {
            return new WindowsRemoteMachineExecutor(this.remoteSharedFolderPath, this.remoteSharedFolderUsername, this.remoteSharedFolderPassword, remoteMachineCredentials, remoteMachinesGraphHelper);
        } else {
            throw new IllegalArgumentException("The remote machine operating system is unknown '"+ remoteMachineCredentials.getOperatingSystemName()+"'.");
        }
    }

    private void writeGraphsToSharedFolder(ILocalMachineFileSystem localMachineFileSystem, Path localSharedFolderPath,
                                           String[] relativeSourceProductFilePaths, Path slaveGraphFile, Path masterGraphFile)
                                           throws Exception {

        Path localOutputRootFolder = recreateOutputSlaveProductsFolder(localSharedFolderPath);

        SlaveProductsInputData slaveProductsInputData = new SlaveProductsInputData(localOutputRootFolder, this.slaveProductsFolderNamePrefix, this.slaveProductsName, this.slaveProductsFormatName);

        RemoteMachinesGraphHelper remoteMachinesGraphHelper = new RemoteMachinesGraphHelper(localMachineFileSystem, slaveProductsInputData, relativeSourceProductFilePaths,
                                                                                            slaveGraphFile, this.continueOnFailure.booleanValue());

        CountDownLatch sharedCounter = new CountDownLatch(this.remoteMachines.length);
        RemoteMachineGraphExecutorRunnable[] remoteMachinesRunnable = new RemoteMachineGraphExecutorRunnable[this.remoteMachines.length];
        for (int i = 0; i < this.remoteMachines.length; i++) {
            remoteMachinesRunnable[i] = buildRemoteMachineGraphExecutorRunnable(this.remoteMachines[i], remoteMachinesGraphHelper, sharedCounter);
        }

        int processorCount = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = null;
        boolean remoteMachinesFinishOnTime = false;
        try {
            if (this.waitingSecondsTimeout > 0) {
                threadPoolExecutor = new ThreadNamePoolExecutor("cep", processorCount);
                for (int i = 0; i < remoteMachinesRunnable.length; i++) {
                    threadPoolExecutor.execute(remoteMachinesRunnable[i]); // start the thread
                }
                boolean countHasReachedZero = sharedCounter.await(this.waitingSecondsTimeout, TimeUnit.SECONDS);
                if (countHasReachedZero) {
                    remoteMachinesFinishOnTime = true;
                } else {
                    // stop the running commands
                    for (int i = 0; i < remoteMachinesRunnable.length; i++) {
                        remoteMachinesRunnable[i].stopRunningCommand();
                    }
                    sharedCounter.await();
                }
            } else {
                if (remoteMachinesRunnable.length > 1) {
                    // start the remote machines from the second position in new threads
                    threadPoolExecutor = new ThreadNamePoolExecutor("cep", processorCount);
                    for (int i = 1; i < remoteMachinesRunnable.length; i++) {
                        threadPoolExecutor.execute(remoteMachinesRunnable[i]); // start the thread
                    }
                }
                // run the first remote machine on the same thread
                remoteMachinesRunnable[0].run(); // run on the current thread
                sharedCounter.await();
                remoteMachinesFinishOnTime = true;
            }
        } finally {
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
            }
        }

        if (remoteMachinesFinishOnTime) {
            int connectedRemoteMachineCount = 0;
            int mountSharedFolderCount = 0;
            List<String> createdOutputProductsRelativeFilePath = new ArrayList<String>();
            for (int i = 0; i < remoteMachinesRunnable.length; i++) {
                createdOutputProductsRelativeFilePath.addAll(remoteMachinesRunnable[i].getCreatedOutputProductsRelativeFilePath());
                RemoteMachineExecutorResult executionResult = remoteMachinesRunnable[i].getExecutionResult();
                if (executionResult == RemoteMachineExecutorResult.CONNECTED_AND_MOUNT_LOCAL_SHARED_FOLDER) {
                    connectedRemoteMachineCount++;
                    mountSharedFolderCount++;
                } else if (executionResult == RemoteMachineExecutorResult.STOP_TO_CONTINUE) {
                    // do nothing
                } else if (executionResult == RemoteMachineExecutorResult.CONNECTED_AND_STOP_TO_CONTINUE) {
                    connectedRemoteMachineCount++;
                } else if (executionResult == RemoteMachineExecutorResult.FAILED_SSH_COONECTION) {
                    // do nothing
                } else if (executionResult == RemoteMachineExecutorResult.CONNECTED_AND_FAILED_MOUNT_LOCAL_SHARED_FOLDER) {
                    connectedRemoteMachineCount++;
                } else if (executionResult == RemoteMachineExecutorResult.FAILED_EXECUTION) {
                    // do nothing
                } else {
                    throw new IllegalArgumentException("Unknown execution result '" + executionResult+ "'.");
                }
            }
            if (connectedRemoteMachineCount == 0) {
                throw new OperatorExecutionException("No remote machine available to execute the source products from the remote shared folder.", ExecutionExceptionType.NO_REMOTE_MACHINE_AVAILABLE);
            }
            if (mountSharedFolderCount == 0) {
                throw new OperatorExecutionException("No remote machine connected to the remote shared folder.", ExecutionExceptionType.NO_REMOTE_MACHINE_MOUNT_SHARED_FOLDER);
            }

            Set<String> failedSourceProductsRelativeFilePath = remoteMachinesGraphHelper.computeFailedSourceProductsRelativeFilePath();
            if (failedSourceProductsRelativeFilePath.size() > 0) {
                throw new OperatorExecutionException("Some source products has not been processed.", ExecutionExceptionType.UNPROCESSED_SOURCE_PRODUCTS);
            }

            boolean canCreateTargetProduct = (masterGraphFile != null);

            if (remoteMachinesGraphHelper.canContinueIfExceptionOccurredOnRemoteMachines()) {
                // ignore the exceptions occurred on the remote machines
                if (canCreateTargetProduct) {
                    logger.log(Level.FINE, "Create the target product file '" + this.masterProductFilePath + "'.");
                    String[] processedOutputProductsRelativeFilePath = new String[createdOutputProductsRelativeFilePath.size()];
                    createdOutputProductsRelativeFilePath.toArray(processedOutputProductsRelativeFilePath);
                    processMasterGraph(localMachineFileSystem, localOutputRootFolder, masterGraphFile, processedOutputProductsRelativeFilePath);
                } else {
                    logger.log(Level.FINE, "The master output product is not created because its attributes are not specified.");
                }
            } else {
                // at least one exception occurred on the remote machines
                if (canCreateTargetProduct) {
                    logger.log(Level.FINE, "The master output product cannot be created because an exception occurred on a remote machine.");
                } else {
                    logger.log(Level.FINE, "An exception occurred on a remote machine.");
                }
            }
        } else {
            String message = "The slave output products have not been created in maximum "+this.waitingSecondsTimeout+" seconds on the remote machines.";
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, message);
            }
            throw new WaitingTimeoutException(message);
        }
    }

    private RemoteMachineGraphExecutorRunnable buildRemoteMachineGraphExecutorRunnable(RemoteMachineProperties remoteMachineCredentials, RemoteMachinesGraphHelper remoteMachinesGraphHelper,
                                                                                       CountDownLatch sharedCounter) {

        AbstractRemoteMachineExecutor remoteMachineExecutor = buildRemoteMachineExecutor(remoteMachineCredentials, remoteMachinesGraphHelper);
        return new RemoteMachineGraphExecutorRunnable(remoteMachineExecutor, sharedCounter);
    }

    private void processMasterGraph(ILocalMachineFileSystem localMachineFileSystem, Path localOutputFolder, Path masterGraphFile,
                                    String[] processedOutputProductsRelativeFilePath)
                                    throws Exception {

        Graph masterGraph = RemoteMachinesGraphHelper.readGraph(masterGraphFile);

        List<Node> readOperatorNodes = new ArrayList<Node>();
        List<Node> writeOperatorNodes = new ArrayList<Node>();
        for (int i = 0; i < masterGraph.getNodeCount(); i++) {
            Node node = masterGraph.getNode(i);
            if (node.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                RemoteMachinesGraphHelper.configureGraphWriteNode(node);
                writeOperatorNodes.add(node);
            } else if (node.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(ReadOp.class))) {
                RemoteMachinesGraphHelper.configureGraphReadNode(node);
                readOperatorNodes.add(node);
            }
        }
        if (writeOperatorNodes.size() == 0) {
            throw new OperatorExecutionException("The master graph must contain the write operator.", ExecutionExceptionType.UNPROCESSED_MASTER_OUTPUT_PRODUCT);
        }
        if (writeOperatorNodes.size() > 1) {
            throw new OperatorExecutionException("The master graph must contain only one write operator. The actual number is " + writeOperatorNodes.size() + ".", ExecutionExceptionType.UNPROCESSED_MASTER_OUTPUT_PRODUCT);
        }
        if (readOperatorNodes.size() == 0) {
            throw new OperatorExecutionException("The master graph must contain at least one read operator. The read operator count must be equal with the slave output product count.", ExecutionExceptionType.UNPROCESSED_MASTER_OUTPUT_PRODUCT);
        }
        if (processedOutputProductsRelativeFilePath.length != readOperatorNodes.size()) {
            throw new OperatorExecutionException("The slave output product file count " + processedOutputProductsRelativeFilePath.length + " is not equal with the master graph read operator count " + readOperatorNodes.size() + ".", ExecutionExceptionType.UNPROCESSED_MASTER_OUTPUT_PRODUCT);
        }

        Path localSharedFolderPath = localOutputFolder.getParent();

        for (int i = 0; i < readOperatorNodes.size(); i++) {
            Node readOperatorNode = readOperatorNodes.get(i);
            String normalizedOutputProductRelativePath = localMachineFileSystem.normalizeFileSeparator(processedOutputProductsRelativeFilePath[i]);
            Path sourceProductPath = localSharedFolderPath.resolve(normalizedOutputProductRelativePath);
            String sourceProductFilePathOnSharedMachine = sourceProductPath.toString();
            if (Files.exists(sourceProductPath)) {
                DomElement fileParam = readOperatorNode.getConfiguration().getChild("file");
                fileParam.setValue(sourceProductFilePathOnSharedMachine);
            } else {
                throw new IllegalStateException("The slave output product file " + sourceProductFilePathOnSharedMachine + " does not exist.");
            }
        }

        Node writeOperatorNode = writeOperatorNodes.get(0);
        DomElement fileParam = writeOperatorNode.getConfiguration().getChild("file");
        fileParam.setValue(this.masterProductFilePath);
        DomElement formatNameParam = writeOperatorNode.getConfiguration().getChild("formatName");
        formatNameParam.setValue(this.masterProductFormatName);

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(masterGraph, new PrintWriterConciseProgressMonitor(System.out));
    }

    public String getMasterProductFilePath() {
        return this.masterProductFilePath;
    }

    private static Path recreateOutputSlaveProductsFolder(Path localSharedFolderPath) {
        String outputSlaveProductsFolderName = "remote-execution-slave-products";
        Path localOutputRootFolder = localSharedFolderPath.resolve(outputSlaveProductsFolderName);

        // recreate the output folder from the remote shared folder containing the slave products
        String failedDeleteFolderMessage = "The folder '"+outputSlaveProductsFolderName+"' containing the output slave products could not be deleted.";
        try {
            if (Files.exists(localOutputRootFolder)) {
                // delete the folder
                FileIOUtils.deleteFolder(localOutputRootFolder);
                // check if the folder still exists
                if (Files.exists(localOutputRootFolder)) {
                    // the output folder has not been deleted
                    throw new OperatorExecutionException(failedDeleteFolderMessage, ExecutionExceptionType.FAILED_TO_DELETE_OUTPUT_SLAVE_PRODUCTS_FOLDER);
                }
            }
            Files.createDirectory(localOutputRootFolder);
            if (!Files.exists(localOutputRootFolder)) {
                // the output folder has not been created
                String failedCreateFolderMessage = "The folder '"+outputSlaveProductsFolderName+"' containing the output slave products could not be created.";
                throw new OperatorExecutionException(failedCreateFolderMessage, ExecutionExceptionType.FAILED_TO_CREATE_OUTPUT_SLAVE_PRODUCTS_FOLDER);
            }

            return localOutputRootFolder;
        } catch (OperatorExecutionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OperatorExecutionException(failedDeleteFolderMessage, exception, ExecutionExceptionType.FAILED_TO_DELETE_OUTPUT_SLAVE_PRODUCTS_FOLDER);
        }
    }

    private static Path buildPath(String pathAsString) throws URISyntaxException {
        int semicolonIndex = pathAsString.indexOf(':');
        if (semicolonIndex > -1) {
            String uriScheme = pathAsString.substring(0, semicolonIndex);
            List<FileSystemProvider> fileSystemProviders = FileSystemProvider.installedProviders();
            for (int i=0; i<fileSystemProviders.size(); i++) {
                FileSystemProvider fileSystemProvider = fileSystemProviders.get(i);
                if (uriScheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                    URI uri = new URI(pathAsString);
                    return fileSystemProvider.getPath(uri);
                }
            }
        }
        return Paths.get(pathAsString);
    }

    private static Path checkGraphFileIfExists(ILocalMachineFileSystem localMachineFileSystem, String normalizedMasterSharedFolderURL,
                                               Path localSharedFolder, String graphFilePathToCheck, boolean isMasterGraph) throws URISyntaxException {

        String exceptionMessagePrefix;
        if (isMasterGraph) {
            exceptionMessagePrefix = "The master graph file '";
        } else {
            exceptionMessagePrefix = "The slave graph file '";
        }

        String normalizedGraphFilePath = localMachineFileSystem.normalizeFileSeparator(graphFilePathToCheck);
        if (localMachineFileSystem.pathStartsWith(normalizedGraphFilePath, normalizedMasterSharedFolderURL)) {
            // the graph file must exists into the master shared folder
            String relativePath = normalizedGraphFilePath.substring(normalizedMasterSharedFolderURL.length());
            Path graphFilePath = localSharedFolder.resolve(relativePath);
            if (!Files.exists(graphFilePath)) {
                throw new OperatorException(exceptionMessagePrefix + normalizedGraphFilePath + "' does not exist into the master shared folder.");
            }
            return graphFilePath;
        }
        // the graph file must exists on the local disk
        Path graphFilePath = buildPath(graphFilePathToCheck);
        if (!Files.exists(graphFilePath)) {
            throw new OperatorException(exceptionMessagePrefix + normalizedGraphFilePath + "' does not exist on the local disk.");
        }
        return graphFilePath;
    }

    private static boolean isMasterHostIdenticalWithLocalHost(String masterHostName) {
        try {
            InetAddress inetAddress = getLocalHostAddress();
            String hostName = inetAddress.getHostName();
            String hostIpAddress = inetAddress.getHostAddress();
            if (masterHostName.equals(hostName) || masterHostName.equals(hostIpAddress)) {
                return true; // the master host name is the same with the running host
            }
        } catch (UnknownHostException exception) {
            logger.log(Level.SEVERE, "Failed to retrieve the local IP address.", exception);
        }
        return false;
    }

    /**
     * https://issues.apache.org/jira/browse/JCS-40
     */
    private static InetAddress getLocalHostAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    public static XppDom buildDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml), new MXParser()), domWriter);
        return domWriter.getConfiguration();
    }

    private static class RemoteMachineGraphExecutorRunnable implements Runnable {

        private static final Logger logger = Logger.getLogger(RemoteMachineGraphExecutorRunnable.class.getName());

        private final AbstractRemoteMachineExecutor remoteMachineExecutor;
        private final CountDownLatch sharedCounter;

        private RemoteMachineExecutorResult executionResult;

        public RemoteMachineGraphExecutorRunnable(AbstractRemoteMachineExecutor remoteMachineExecutor, CountDownLatch sharedCounter) {
            this.remoteMachineExecutor = remoteMachineExecutor;
            this.sharedCounter = sharedCounter;
        }

        @Override
        public void run() {
            try {
                this.executionResult = this.remoteMachineExecutor.doExecute();
            } catch (Exception exception) {
                this.executionResult = RemoteMachineExecutorResult.FAILED_EXECUTION;
                this.remoteMachineExecutor.setExceptionOccurredOnRemoteMachine(exception);
                logger.log(Level.SEVERE, this.remoteMachineExecutor.buildFailedLogMessage(), exception);
            } finally {
                this.sharedCounter.countDown();
            }
        }

        public List<String> getCreatedOutputProductsRelativeFilePath() {
            return this.remoteMachineExecutor.getCreatedOutputProductsRelativeFilePath();
        }

        public final void stopRunningCommand() throws Exception {
            this.remoteMachineExecutor.stopRunningCommand();
        }

        public RemoteMachineExecutorResult getExecutionResult() {
            return this.executionResult;
        }
    }

    /**
     * Collocation operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(RemoteExecutionOp.class);
        }
    }
}
