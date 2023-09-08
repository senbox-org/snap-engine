package com.bc.ceres.core;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ProcessObserverTest {
    private static final String JAVA_HOME = System.getProperty("java.home", ".");
    static final String JAVA_EXEC_PATH = JAVA_HOME + "/bin/java";
    static String classPath;

    @BeforeClass
    public static void setUp() throws Exception {
        URL location = TestExecutable.class.getProtectionDomain().getCodeSource().getLocation();
        classPath = new File(location.toURI()).getCanonicalPath();
    }

    @Test
    public void testJavaProcessMissingArg() throws Exception {
        final String commandLine = String.format(getJavaExecPath() + " -cp %s %s 2", getClassPath(), TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        new ProcessObserver(process).setHandler(handler).start();
        assertTrue(handler.started);
        // assertEquals("", handler.out); // leads to error on travis something is written to out
        // assertEquals("Usage: TestExecutable <seconds> <steps>\n", handler.err); // leads to error on travis something is written to out
        assertTrue(handler.ended);
        assertEquals(1, handler.exitCode.intValue());
    }

    @Test
    public void testJavaProcessCancel() throws Exception {
        final String commandLine = String.format(getJavaExecPath() + " -cp %s %s 10 2", getClassPath(), TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        final ProcessObserver.ObservedProcess observedProcess = new ProcessObserver(process)
                .setMode(ProcessObserver.Mode.NON_BLOCKING)
                .setHandler(handler).start();
        Thread.sleep(250);
        observedProcess.cancel();

        Thread.sleep(500);

        assertTrue(handler.started);

        assertEquals("Start\n", handler.out);
        assertEquals("", handler.err);
        assertTrue(handler.ended);
        assertNull(handler.exitCode);
    }

    protected String getJavaExecPath() {
        //Add "" to JAVA path if it has blank spaces
        if (JAVA_EXEC_PATH.contains(" ")) {
            return "\"" + JAVA_EXEC_PATH + "\"";
        }

        return JAVA_EXEC_PATH;
    }

    protected String getClassPath() {
        //Add "" to claspath if it has blank spaces
        if (classPath.contains(" ")) {
            return "\"" + classPath + "\"";
        }

        return classPath;
    }

    static class MyHandler implements ProcessObserver.Handler {
        boolean started = false;
        String out = "";
        String err = "";
        boolean ended = false;
        Integer exitCode;

        @Override
        public void onObservationStarted(ProcessObserver.ObservedProcess process, ProgressMonitor pm) {
            started = true;
        }

        @Override
        public void onStdoutLineReceived(ProcessObserver.ObservedProcess process, String line, ProgressMonitor pm) {
            out += line + "\n";
        }

        @Override
        public void onStderrLineReceived(ProcessObserver.ObservedProcess process, String line, ProgressMonitor pm) {
            err += line + "\n";
        }

        @Override
        public void onObservationEnded(ProcessObserver.ObservedProcess process, Integer exitCode, ProgressMonitor pm) {
            ended = true;
            this.exitCode = exitCode;
        }
    }
}
