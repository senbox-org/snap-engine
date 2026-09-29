package org.esa.snap.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ActivatorsTest {

    @Test
    public void testActivatorsAreCalledInCorrectOrder() throws Exception {

        trace = "";
        Engine engine = Engine.start(false);
        Assert.assertEquals("started A4;started A3;started A1;started A2;", trace);

        trace = "";
        engine.stop();
        Assert.assertEquals("stopped A2;stopped A1;stopped A3;stopped A4;", trace);
    }

    @Test
    public void testDeferrableActivatorsAreStartedByExecutor() throws Exception {

        trace = "";
        final List<Runnable> deferredTasks = new ArrayList<>();
        Engine engine = Engine.start(false, deferredTasks::add);
        try {
            Assert.assertEquals("started A3;started A1;started A2;", trace);
            Assert.assertEquals(1, deferredTasks.size());

            trace = "";
            deferredTasks.get(0).run();
            Assert.assertEquals("started A4;", trace);
        } finally {
            engine.stop();
        }
    }

    public static String trace = "";

    public static class Base implements Activator {
        @Override
        public void start() {
            trace += "started " + getClass().getSimpleName() + ";";
        }

        @Override
        public void stop() {
            trace += "stopped " + getClass().getSimpleName() + ";";
        }
    }

    public static class A1 extends Base {
    }

    public static class A2 extends Base {
        @Override
        public int getStartLevel() {
            return 100;
        }
    }

    public static class A3 extends Base {
        @Override
        public int getStartLevel() {
            return -100;
        }
    }

    public static class A4 extends Base {
        @Override
        public int getStartLevel() {
            return -200;
        }

        @Override
        public boolean isDeferrable() {
            return true;
        }
    }
}
