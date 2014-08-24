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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

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
        if (!jpyConfigFile.exists()) {
            String python = System.getProperty("beam.pythonExecutable", "python");
            String script = "configtool.py";
            String architecture = System.getProperty("os.arch");
            System.out.printf("Executing: \"%s\" %s %s\n", python, script, architecture);
            try {
                Process process = new ProcessBuilder()
                        .command(python, script, architecture)
                        .directory(beampyDir).start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new OperatorException("Python configuration failed with executable '" + python + "'");
                }
            } catch (IOException | InterruptedException e) {
                throw new OperatorException("Python configuration failed with executable " + python + "'", e);
            }
        }

        if (jpyConfigFile.exists()) {
            System.setProperty("jpy.config", jpyConfigFile.getPath());
        } else {
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
