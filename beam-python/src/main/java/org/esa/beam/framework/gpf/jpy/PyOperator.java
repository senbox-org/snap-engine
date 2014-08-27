package org.esa.beam.framework.gpf.jpy;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * An operator which uses Python code to process data products.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
@OperatorMetadata(alias = "PyOp",
                  description = "Uses Python code to process data products",
                  version = "0.8",
                  authors = "N. Fomferra",
                  internal = true)
public class PyOperator extends Operator {

    @Parameter(description = "Path to the Python module(s). Can be either an absolute path or relative to the current working directory.", defaultValue = ".")
    private String pythonModulePath;

    @Parameter(description = "Name of the Python module.")
    private String pythonModuleName;

    /**
     * Name of the Python class which implements the {@link org.esa.beam.framework.gpf.jpy.PyOperator.PythonProcessor} interface.
     */
    @Parameter(description = "Name of the Python class which implements the operator. Please refer to the BEAM help for details.")
    private String pythonClassName;

    private static boolean globalPythonInit;
    private transient PyModule pyModule;
    private transient PythonProcessor pythonProcessor;
    private static File beampyDir;


    public String getPythonModulePath() {
        return pythonModulePath;
    }

    public void setPythonModulePath(String pythonModulePath) {
        this.pythonModulePath = pythonModulePath;
    }

    public String getPythonModuleName() {
        return pythonModuleName;
    }

    public void setPythonModuleName(String pythonModuleName) {
        this.pythonModuleName = pythonModuleName;
    }

    public String getPythonClassName() {
        return pythonClassName;
    }

    public void setPythonClassName(String pythonClassName) {
        this.pythonClassName = pythonClassName;
    }

    private File getResourceFile(String resourcePath) {
        URL resourceUrl = getClass().getResource(resourcePath);
        System.out.println("resourceUrl = " + resourceUrl);
        if (resourceUrl != null) {
            try {
                File resourceFile = new File(resourceUrl.toURI());
                System.out.println("resourceFile = " + resourceFile);
                if (resourceFile.exists()) {
                    return resourceFile;
                }
            } catch (URISyntaxException e) {
                // mmmmh
            }
        }
        return null;
    }


    @Override
    public void initialize() throws OperatorException {
        if (pythonModuleName == null || pythonModuleName.isEmpty()) {
            throw new OperatorException("Missing parameter 'pythonModuleName'");
        }
        if (pythonClassName == null || pythonClassName.isEmpty()) {
            throw new OperatorException("Missing value for parameter 'pythonClassName'");
        }

        if (!globalPythonInit) {
            initPython();
        }

        synchronized (PyLib.class) {
            extendSysPath(pythonModulePath);

            String code = String.format("if '%s' in globals(): del %s", pythonModuleName, pythonModuleName);
            PyLib.execScript(code);

            pyModule = PyModule.importModule(pythonModuleName);
            PyObject pythonProcessorImpl = pyModule.call(pythonClassName);
            pythonProcessor = pythonProcessorImpl.createProxy(PythonProcessor.class);
            pythonProcessor.initialize(this);
        }
    }

    private void initPython() {
        beampyDir = getResourceFile("/beampy");
        if (beampyDir == null) {
            throw new OperatorException("Python can only be run from unpacked modules");
        }
        System.out.println("beampyDir: " + beampyDir);

        File jpyConfigFile = new File(beampyDir, "jpyconfig.properties");


        String pythonExecutable = System.getProperty("beam.pythonExecutable");

        boolean mustConfigure = false;

        if (!jpyConfigFile.exists()) {
            mustConfigure = true;
        } else {
            if (pythonExecutable != null) {
                Properties properties = new Properties();
                try {
                    try (FileReader reader = new FileReader(jpyConfigFile)) {
                        properties.load(reader);
                        String jpyPythonExecutable = properties.getProperty("jpy.pythonExecutable");
                        if (jpyPythonExecutable != null && !jpyPythonExecutable.equals(pythonExecutable)) {
                            mustConfigure = true;
                        }
                    }
                } catch (IOException e) {
                    // mmmh...
                }
            }
        }

        if (mustConfigure) {
            if (pythonExecutable == null) {
                pythonExecutable = "python";
            }
            configurePythonBridge(pythonExecutable);
        }

        if (jpyConfigFile.exists()) {
            System.setProperty("jpy.config", jpyConfigFile.getPath());
        } else {
            // todo - read "configtool.log" and display errors
            throw new OperatorException("Python configuration incomplete.\nMissing " + jpyConfigFile);
        }

        synchronized (PyLib.class) {
            if (!globalPythonInit) {
                //PyLib.Diag.setFlags(PyLib.Diag.F_ALL);
                String pythonVersion = PyLib.getPythonVersion();
                System.out.println("Running Python " + pythonVersion);
                if (!PyLib.isPythonRunning()) {
                    PyLib.startPython(beampyDir.getPath());
                } else {
                    extendSysPath(beampyDir.getPath());
                }
                globalPythonInit = true;
            }
        }
    }

    private void configurePythonBridge(String pythonExecutable) {
        // "java.home" is always present
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("beampyutil.py");
        command.add("--force");
        command.add("--log_file");
        command.add("beampyutil.log");
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            command.add( "--java_home");
            command.add(javaHome);
        }
        String osArch = System.getProperty("os.arch");  // "os.arch" is always present
        if (osArch != null) {
            command.add("--req_arch");
            command.add(osArch);
        }
        String commandLine = toCommandLine(command);
        System.out.printf("Executing command: [%s]\n", commandLine);
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .directory(beampyDir).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new OperatorException(String.format("Python configuration failed.\nCommand [%s]\nfailed with return code %s.", commandLine, exitCode));
            }
        } catch (IOException | InterruptedException e) {
            throw new OperatorException(String.format("Python configuration failed.\nCommand [%s]\nfailed with exception %s.", commandLine, e.getMessage()), e);
        }
    }

    private static String toCommandLine(List<String> command) {
        StringBuilder sb = new StringBuilder();
        for (String arg : command) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(arg.contains(" ") ? String.format("\"%s\"", arg) : arg);
        }
        return sb.toString();
    }

    private void extendSysPath(String path) {
        if (path != null) {
            String code = String.format("" +
                                        "import sys;\n" +
                                        "p = '%s';\n" +
                                        "if not p in sys.path: sys.path.append(p)",
                                        path.replace("\\", "\\\\"));
            PyLib.execScript(code);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        synchronized (PyLib.class) {
            //System.out.println("computeTileStack: thread = " + Thread.currentThread());
            //PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
            pythonProcessor.compute(this, targetTiles, targetRectangle);
            //PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
        }
    }

    @Override
    public void dispose() {
        synchronized (PyLib.class) {
            //System.out.println("dispose: thread = " + Thread.currentThread());
            pythonProcessor.dispose(this);
        }
    }

    /**
     * The interface that the given Python class must implement.
     */
    public interface PythonProcessor {
        /**
         * Initialize the operator.
         *
         * @param operator The GPF operator which called the Python code.
         */
        void initialize(Operator operator);

        /**
         * Compute the tiles associated with the given bands.
         *
         * @param operator        The GPF operator which called the Python code.
         * @param targetTiles     a mapping from {@link Band} objects to {@link Tile} objects.
         * @param targetRectangle the target rectangle to process in pixel coordinates.
         */
        void compute(Operator operator, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

        /**
         * Disposes the operator and all the resources associated with it.
         *
         * @param operator The GPF operator which called the Python code.
         */
        void dispose(Operator operator);
    }

}
