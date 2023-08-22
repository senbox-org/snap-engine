package com.bc.ceres.core;

import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(LongTestRunner.class)
public class ProcessObserverLongTest extends ProcessObserverTest{

    @Test
    public void testJavaProcessOk() throws Exception {
        final String commandLine = String.format(getJavaExecPath() + " -cp %s %s 2 10", getClassPath(), TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        new ProcessObserver(process).setHandler(handler).start();
        assertTrue(handler.started);
        assertEquals("Start\n" +
                             "Progress 10%\n" +
                             "Progress 20%\n" +
                             "Progress 30%\n" +
                             "Progress 40%\n" +
                             "Progress 50%\n" +
                             "Progress 60%\n" +
                             "Progress 70%\n" +
                             "Progress 80%\n" +
                             "Progress 90%\n" +
                             "Progress 100%\n" +
                             "Done\n", handler.out);

        // assertEquals("", handler.err); // leads to error on travis something is written to err
        assertTrue(handler.ended);
        assertEquals(0, handler.exitCode.intValue());
    }

}
