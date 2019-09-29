package org.esa.snap.remote.execution.machines.executors;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.remote.execution.exceptions.ExecutionExceptionType;
import org.esa.snap.remote.execution.exceptions.OperatorExecutionException;
import org.esa.snap.remote.execution.file.system.ILocalMachineFileSystem;
import org.esa.snap.remote.execution.file.system.IRemoteMachineFileSystem;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.ReadOp;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.engine_utilities.util.FileIOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jcoravu on 10/1/2019.
 */
public class RemoteMachinesGraphHelper {

    private final List<Node> readOperatorNodes;
    private final Node writeOperatorNode;
    private final Graph graph;
    private final Path slaveGraphFile;
    private final Set<String> processedSourceProductFiles;
    private final Map<RemoteMachineProperties, Set<String>> invalidRemoteMachine;
    private final boolean continueOnFailure;
    private final AtomicBoolean exceptionOccurred;
    private final String[] relativeSourceProductFilePaths;
    private final SlaveProductsInputData slaveProductsInputData;
    private final ILocalMachineFileSystem localMachineFileSystem;

    private int sourceProductFilesIndex;

    public RemoteMachinesGraphHelper(ILocalMachineFileSystem localMachineFileSystem, SlaveProductsInputData slaveProductsInputData, String[] relativeSourceProductFilePaths,
                                     Path slaveGraphFile, boolean continueOnFailure)
                                     throws IOException, GraphException {

        this.localMachineFileSystem = localMachineFileSystem;
        this.slaveProductsInputData = slaveProductsInputData;
        this.relativeSourceProductFilePaths = relativeSourceProductFilePaths;
        this.continueOnFailure = continueOnFailure;

        this.slaveGraphFile = slaveGraphFile;
        this.exceptionOccurred = new AtomicBoolean(false);

        this.sourceProductFilesIndex = 0;

        this.processedSourceProductFiles = new HashSet<String>();
        this.invalidRemoteMachine = new HashMap<RemoteMachineProperties, Set<String>>();

        this.graph = readGraph(this.slaveGraphFile);

        this.readOperatorNodes = new ArrayList<Node>();
        List<Node> writeOperatorNodes = new ArrayList<Node>();
        for (int i = 0; i < this.graph.getNodeCount(); i++) {
            Node node = this.graph.getNode(i);
            if (node.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                configureGraphWriteNode(node);
                writeOperatorNodes.add(node);
            } else if (node.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(ReadOp.class))) {
                configureGraphReadNode(node);
                this.readOperatorNodes.add(node);
            }
        }
        if (writeOperatorNodes.size() == 0) {
            throw new OperatorExecutionException("The slave graph must contain the write operator.", ExecutionExceptionType.UNPROCESSED_SLAVE_OUTPUT_PRODUCTS);
        }
        if (writeOperatorNodes.size() > 1) {
            throw new OperatorExecutionException("The slave graph must contain only one write operator. The actual number is " + writeOperatorNodes.size() + ".", ExecutionExceptionType.UNPROCESSED_SLAVE_OUTPUT_PRODUCTS);
        }
        if (this.readOperatorNodes.size() == 0) {
            throw new OperatorExecutionException("The slave graph must contain at least one read operator. The read operator count must be equal with the source product count.", ExecutionExceptionType.UNPROCESSED_SLAVE_OUTPUT_PRODUCTS);
        }
        if (this.relativeSourceProductFilePaths.length % this.readOperatorNodes.size() != 0) {
            throw new OperatorExecutionException("The source product file count "+this.relativeSourceProductFilePaths.length+" must be multiple of the graph read operator count "+this.readOperatorNodes.size()+".", ExecutionExceptionType.UNPROCESSED_SLAVE_OUTPUT_PRODUCTS);
        }

        this.writeOperatorNode = writeOperatorNodes.get(0);
    }

    public void setExceptionOccurredOnRemoteMachine(Exception exception) {
        synchronized (this.exceptionOccurred) {
            this.exceptionOccurred.set(true);
        }
    }

    private boolean isExceptionOccurredOnRemoteMachine() {
        synchronized (this.exceptionOccurred) {
            return this.exceptionOccurred.get();
        }
    }

    public boolean canContinueIfExceptionOccurredOnRemoteMachines() {
        if (isExceptionOccurredOnRemoteMachine()) {
            return this.continueOnFailure;
        }
        return true;
    }

    public synchronized Set<String> computeFailedSourceProductsRelativeFilePath() {
        Set<String> failedRelativeSourceProductFilePaths = new HashSet<String>();
        Iterator<Map.Entry<RemoteMachineProperties, Set<String>>> it = this.invalidRemoteMachine.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RemoteMachineProperties, Set<String>> entry = it.next();
            Set<String> failedSourceProductFilePaths = entry.getValue();
            failedRelativeSourceProductFilePaths.addAll(failedSourceProductFilePaths);
        }
        return failedRelativeSourceProductFilePaths;
    }

    public synchronized void addUnprocessedGraphSourceProducts(RemoteMachineProperties activeRemoteMachine, RemoteMachineExecutorInputData previousFailedSourceProducts)
                                                                      throws IOException, GraphException {

        Set<String> failedSourceProducts = this.invalidRemoteMachine.get(activeRemoteMachine);
        if (failedSourceProducts == null) {
            failedSourceProducts = new HashSet<String>();
            this.invalidRemoteMachine.put(activeRemoteMachine, failedSourceProducts);
        }
        String[] graphRelativeSourceProductFilePaths = previousFailedSourceProducts.getGraphRelativeSourceProductFilePaths();
        for (int i = 0; i < graphRelativeSourceProductFilePaths.length; i++) {
            this.processedSourceProductFiles.remove(graphRelativeSourceProductFilePaths[i]);
            failedSourceProducts.add(graphRelativeSourceProductFilePaths[i]);
        }
        deleteGraphLocalFolder(previousFailedSourceProducts.getOutputProductRelativeFilePath());
    }

    public synchronized RemoteMachineExecutorInputData computeNextGraphToRun(RemoteMachineProperties activeRemoteMachine, IRemoteMachineFileSystem remoteMachineFileSystem)
                                                                    throws IOException, GraphException {

        if (this.processedSourceProductFiles.size() < this.relativeSourceProductFilePaths.length) {
            String[] unprocessedRelativeSourceProductFilePaths = findNextUnprocessedSourceProductFiles(activeRemoteMachine, this.readOperatorNodes.size());
            if (unprocessedRelativeSourceProductFilePaths != null) {
                String remoteOutputProductFolderName = "";
                for (int i = 0; i < this.readOperatorNodes.size(); i++) {
                    if (unprocessedRelativeSourceProductFilePaths[i] == null) {
                        throw new IllegalStateException("Incomplete unprocessed source product files.");
                    }

                    int productFileIndex = -1;
                    for (int k = 0; k < this.relativeSourceProductFilePaths.length; k++) {
                        if (this.relativeSourceProductFilePaths[k].equals(unprocessedRelativeSourceProductFilePaths[i])) {
                            productFileIndex = k;
                            break;
                        }
                    }
                    if (productFileIndex >= 0) {
                        if (remoteOutputProductFolderName.length() > 0) {
                            remoteOutputProductFolderName += "-";
                        }
                        remoteOutputProductFolderName += Integer.toString(productFileIndex + 1);

                        String relativeFilePath = remoteMachineFileSystem.normalizeFileSeparator(unprocessedRelativeSourceProductFilePaths[i]);
                        if (relativeFilePath.charAt(0) != remoteMachineFileSystem.getFileSeparatorChar()) {
                            relativeFilePath = remoteMachineFileSystem.getFileSeparatorChar() + relativeFilePath;
                        }
                        String sourceProductFilePath = remoteMachineFileSystem.normalizeFileSeparator(activeRemoteMachine.getSharedFolderPath() + relativeFilePath);

                        Node readOperatorNode = this.readOperatorNodes.get(i);
                        DomElement fileParam = readOperatorNode.getConfiguration().getChild("file");
                        fileParam.setValue(sourceProductFilePath);
                    } else {
                        throw new IllegalStateException("The graph source product file does not exist among the source product files.");
                    }
                }

                String localOutputFolderName = this.slaveProductsInputData.getOutputRootFolder().getFileName().toString();
                String slaveGraphFileName = this.slaveGraphFile.getFileName().toString();

                String oututFolderRelativePath = remoteOutputProductFolderName;
                if (!StringUtils.isBlank(this.slaveProductsInputData.getProductsFolderNamePrefix())) {
                    oututFolderRelativePath = this.slaveProductsInputData.getProductsFolderNamePrefix() + "-" + remoteOutputProductFolderName;
                }

                String outputFolderRelativePath = localOutputFolderName + "/" + oututFolderRelativePath;

                String outputProductRelativeFilePath = remoteMachineFileSystem.normalizeFileSeparator(outputFolderRelativePath + "/" + this.slaveProductsInputData.getProductsName());

                String outputProductFilePathOnRemoteMachine = remoteMachineFileSystem.normalizeFileSeparator(activeRemoteMachine.getSharedFolderPath() + "/" + outputProductRelativeFilePath);

                DomElement fileParam = this.writeOperatorNode.getConfiguration().getChild("file");
                fileParam.setValue(outputProductFilePathOnRemoteMachine);

                if (!StringUtils.isBlank(this.slaveProductsInputData.getProductsFormatName())) {
                    DomElement formatNameParam = this.writeOperatorNode.getConfiguration().getChild("formatName");
                    formatNameParam.setValue(this.slaveProductsInputData.getProductsFormatName());
                }

                Path graphLocalFolderPath = deleteGraphLocalFolder(outputProductRelativeFilePath);

                Files.createDirectory(graphLocalFolderPath);
                if (!Files.exists(graphLocalFolderPath)) {
                    throw new IllegalStateException("Failed to create the graph folder path '" + graphLocalFolderPath.toString()+ "' into the local shared folder.");
                }

                Path slaveGraphFilePath = graphLocalFolderPath.resolve(slaveGraphFileName);
                writeGraph(this.graph, slaveGraphFilePath);

                String remoteGraphRelativeFilePath = remoteMachineFileSystem.normalizeFileSeparator(outputFolderRelativePath + "/" + slaveGraphFileName);

                return new RemoteMachineExecutorInputData(remoteGraphRelativeFilePath, outputProductRelativeFilePath, unprocessedRelativeSourceProductFilePaths);
            }
        }
        return null;
    }

    public Path getLocalOutputFolder() {
        return this.slaveProductsInputData.getOutputRootFolder();
    }

    public ILocalMachineFileSystem getLocalMachineFileSystem() {
        return this.localMachineFileSystem;
    }

    private Path deleteGraphLocalFolder(String outputProductRelativeFilePath) throws IOException {
        String relativePath = this.localMachineFileSystem.normalizeFileSeparator(outputProductRelativeFilePath);
        Path outputProductFilePath = this.slaveProductsInputData.getOutputRootFolder().getParent().resolve(relativePath);
        Path graphLocalFolderPath = outputProductFilePath.getParent();
        if (Files.exists(graphLocalFolderPath)) {
            // delete the folder
            FileIOUtils.deleteFolder(graphLocalFolderPath);
            // check if the folder still exists
            if (Files.exists(graphLocalFolderPath)) {
                throw new IllegalStateException("Failed to delete the graph folder path '" + graphLocalFolderPath.toString()+ "'.");
            }
        }
        return graphLocalFolderPath;
    }

    private String[] findNextUnprocessedSourceProductFiles(RemoteMachineProperties activeRemoteMachine, int count) {
        Set<String> failedRelativeSourceProductFilePaths = this.invalidRemoteMachine.get(activeRemoteMachine);
        String[] graphRelativeSourceProductFilePaths = new String[count];
        for (int i=0; i<graphRelativeSourceProductFilePaths.length; i++) {
            String relativeSourceProductFilePathToProcess = null;
            for (int k = 0; k < this.relativeSourceProductFilePaths.length && relativeSourceProductFilePathToProcess == null; k++) {
                String currentRelativeSourceProductFilePath = this.relativeSourceProductFilePaths[this.sourceProductFilesIndex];
                this.sourceProductFilesIndex++;
                this.sourceProductFilesIndex = this.sourceProductFilesIndex % this.relativeSourceProductFilePaths.length;
                if (failedRelativeSourceProductFilePaths == null || !failedRelativeSourceProductFilePaths.contains(currentRelativeSourceProductFilePath)) {
                    // remove the source product file path from invalid remote machine map
                    Iterator<Map.Entry<RemoteMachineProperties, Set<String>>> it = this.invalidRemoteMachine.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<RemoteMachineProperties, Set<String>> entry = it.next();
                        Set<String> failedSourceProductFilePaths = entry.getValue();
                        failedSourceProductFilePaths.remove(currentRelativeSourceProductFilePath);
                    }

                    if (this.processedSourceProductFiles.add(currentRelativeSourceProductFilePath)) {
                        relativeSourceProductFilePathToProcess = currentRelativeSourceProductFilePath;
                    }
                }
            }
            if (relativeSourceProductFilePathToProcess == null) {
                return null; // no available source product for the active remote machine
            } else {
                graphRelativeSourceProductFilePaths[i] = relativeSourceProductFilePathToProcess;
            }
        }
        return graphRelativeSourceProductFilePaths;
    }

    public static Graph readGraph(Path graphFilePath) throws IOException, GraphException {
        InputStream inputStream = Files.newInputStream(graphFilePath);
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            try {
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                try {
                    return GraphIO.read(bufferedReader);
                } finally {
                    bufferedReader.close();
                }
            } finally {
                inputStreamReader.close();
            }
        } finally {
            inputStream.close();
        }
    }

    static void writeGraph(Graph graph, Path graphFilePath) throws IOException, GraphException {
        OutputStream outputStream = Files.newOutputStream(graphFilePath);
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                try {
                    GraphIO.write(graph, bufferedWriter);
                } finally {
                    bufferedWriter.close();
                }
            } finally {
                outputStreamWriter.close();
            }
        } finally {
            outputStream.close();
        }
    }

    public static void configureGraphReadNode(Node node) {
        DomElement config = node.getConfiguration();
        if (config == null) {
            config = new XppDomElement("parameters");
            node.setConfiguration(config);
        }
        DomElement fileParam = config.getChild("file");
        if (fileParam == null) {
            fileParam = new XppDomElement("file");
            config.addChild(fileParam);
        }
    }

    public static void configureGraphWriteNode(Node node) {
        DomElement config = node.getConfiguration();
        if (config == null) {
            config = new XppDomElement("parameters");
            node.setConfiguration(config);
        }
        DomElement fileParam = config.getChild("file");
        if (fileParam == null) {
            fileParam = new XppDomElement("file");
            config.addChild(fileParam);
        }
        DomElement formatNameParam = config.getChild("formatName");
        if (formatNameParam == null) {
            formatNameParam = new XppDomElement("formatName");
            config.addChild(formatNameParam);
        }
        formatNameParam.setValue("BEAM-DIMAP");
    }
}
