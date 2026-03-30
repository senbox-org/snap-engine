package com.bc.ceres.util;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;


public class CleanerRegistryTest {


    @Test
    @STTM("SNAP-4105")
    public void test_singleton() {
        CleanerRegistry cR1 = CleanerRegistry.getInstance();
        CleanerRegistry cR2 = CleanerRegistry.getInstance();

        assertSame(cR1, cR2);
    }

    @Test
    @STTM("SNAP-4105")
    public void test_cleanup_runsActionsOnce_inLifoOrder() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        List<Integer> calls = new ArrayList<>();

        reg.register(owner, new RecordingState(calls, 1));
        reg.register(owner, new RecordingState(calls, 2));
        reg.register(owner, new RecordingState(calls, 3));

        reg.cleanup(owner);

        assertEquals(List.of(3, 2, 1), calls);
    }

    @Test
    @STTM("SNAP-4105")
    public void test_cleanup_isIdempotent() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        AtomicBoolean ran = new AtomicBoolean(false);
        reg.register(owner, new RunOnlyOnceState(ran));

        reg.cleanup(owner);
        reg.cleanup(owner);

        assertTrue(ran.get());
    }

    @Test
    @STTM("SNAP-4105")
    public void test_cleanup_runsAllEvenIfOneThrows() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        AtomicBoolean closed1 = new AtomicBoolean(false);
        AtomicBoolean closed2 = new AtomicBoolean(false);

        reg.register(owner, new AtomicBooleanState(closed1));
        reg.register(owner, new ThrowingState(closed2));

        reg.cleanup(owner);

        assertTrue(closed1.get());
        assertTrue(closed2.get());
    }

    @Test
    @STTM("SNAP-4105")
    public void test_cleanerFallback_runs_whenOwnerBecomesUnreachable() throws Exception {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        CountDownLatch latch = new CountDownLatch(1);
        Object owner = new Object();
        WeakReference<Object> ref = new WeakReference<>(owner);

        reg.register(owner, new LatchState(latch));

        owner = null;
        forceGcUntilCleared(ref);

        assertTrue("Cleaner fallback did not run", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsLambda() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        try {
            reg.register(owner, () -> {});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must not be a lambda") || e.getMessage().contains("synthetic"));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsAnonymousClass() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        try {
            reg.register(owner, new CleanUpState() {
                @Override
                public void run() {
                }
            });
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("anonymous"));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsLocalClass() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        class LocalState implements CleanUpState {
            @Override
            public void run() {
            }
        }

        try {
            reg.register(owner, new LocalState());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("local class"));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsNonStaticMemberClass() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        try {
            reg.register(owner, new NonStaticMemberState());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("static nested class or top-level class"));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsDirectOwnerReference() {
        CleanerRegistry reg = CleanerRegistry.newForTest();
        Object owner = new Object();

        try {
            reg.register(owner, new OwnerReferencingState(owner));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("direct reference to the owner"));
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsNullOwner() {
        CleanerRegistry reg = CleanerRegistry.newForTest();

        try {
            reg.register(null, new NoOpState());
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("owner", e.getMessage());
        }
    }

    @Test
    @STTM("SNAP-4105")
    public void test_register_rejectsNullState() {
        CleanerRegistry reg = CleanerRegistry.newForTest();

        try {
            reg.register(new Object(), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("CleanUpState", e.getMessage());
        }
    }

    private static void forceGcUntilCleared(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 200 && ref.get() != null; i++) {
            System.gc();
            byte[] pressure = new byte[256 * 1024];
            if (pressure.length == 0) {
                throw new AssertionError();
            }
            Thread.sleep(10);
        }
        assertNull("Owner not GC'ed in test", ref.get());
    }

    private static final class RecordingState implements CleanUpState {
        private final List<Integer> calls;
        private final int value;

        private RecordingState(List<Integer> calls, int value) {
            this.calls = calls;
            this.value = value;
        }

        @Override
        public void run() {
            calls.add(value);
        }
    }

    private static final class RunOnlyOnceState implements CleanUpState {
        private final AtomicBoolean ran;

        private RunOnlyOnceState(AtomicBoolean ran) {
            this.ran = ran;
        }

        @Override
        public void run() {
            if (!ran.compareAndSet(false, true)) {
                throw new AssertionError("cleanup action ran more than once");
            }
        }
    }

    private static final class AtomicBooleanState implements CleanUpState {
        private final AtomicBoolean flag;

        private AtomicBooleanState(AtomicBoolean flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            flag.set(true);
        }
    }

    private static final class ThrowingState implements CleanUpState {
        private final AtomicBoolean flag;

        private ThrowingState(AtomicBoolean flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            flag.set(true);
            throw new RuntimeException("boom");
        }
    }

    private static final class LatchState implements CleanUpState {
        private final CountDownLatch latch;

        private LatchState(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            latch.countDown();
        }
    }

    private static final class OwnerReferencingState implements CleanUpState {
        private final Object owner;

        private OwnerReferencingState(Object owner) {
            this.owner = owner;
        }

        @Override
        public void run() {
        }
    }

    private static final class NoOpState implements CleanUpState {
        @Override
        public void run() {
        }
    }

    private final class NonStaticMemberState implements CleanUpState {
        @Override
        public void run() {
        }
    }
}