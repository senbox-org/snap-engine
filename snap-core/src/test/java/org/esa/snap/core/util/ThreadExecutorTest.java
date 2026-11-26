package org.esa.snap.core.util;

import com.bc.ceres.annotation.STTM;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ThreadExecutorTest {

    @Test
    public void testThreadPool() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor();

        final ThreadRunnable runnable1 = new ThreadRunnable() {
            @Override
            public void process() {
                System.out.println("Thread 1");
            }
        };
        executor.execute(runnable1);

        final ThreadRunnable runnable2 = new ThreadRunnable() {
            @Override
            public void process() {
                System.out.println("Thread 2");
            }
        };
        executor.execute(runnable2);

        executor.complete();
    }

    @Test
    public void testException() throws Exception {
        try {
            final ThreadExecutor executor = new ThreadExecutor();

            final ThreadRunnable runnable1 = new ThreadRunnable() {
                @Override
                public void process() throws Exception {
                    throw new Exception("propagate this");
                }
            };
            executor.execute(runnable1);

            executor.complete();

        } catch (Exception e) {
            assert(e.getMessage().equals("propagate this"));
        }
    }

    @Test
    @STTM("SNAP-4391")
    public void failureOfAnEarlierTaskIsRethrown() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor(2);
        final IOException failure = new IOException("first task failed");
        final CountDownLatch firstDone = new CountDownLatch(1);
        executor.execute(new ThreadRunnable() {
            @Override
            public void process() throws Exception {
                try {
                    throw failure;
                } finally {
                    firstDone.countDown();
                }
            }
        });
        executor.execute(new ThreadRunnable() {
            @Override
            public void process() throws Exception {
                firstDone.await(10, TimeUnit.SECONDS);   // the last submitted task succeeds, after the failure
            }
        });

        final IOException thrown = assertThrows(IOException.class, executor::complete);
        assertSame("the task's own exception is rethrown, type and identity preserved", failure, thrown);
    }

    @Test
    @STTM("SNAP-4391")
    public void errorInATaskIsRethrownNotSwallowed() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor(2);
        final OutOfMemoryError oom = new OutOfMemoryError("simulated");
        final ThreadRunnable failing = new ThreadRunnable() {
            @Override
            public void process() {
                throw oom;
            }
        };
        executor.execute(failing);
        executor.execute(new ThreadRunnable() {
            @Override
            public void process() {
            }
        });

        final OutOfMemoryError thrown = assertThrows(OutOfMemoryError.class, executor::complete);
        assertSame(oom, thrown);
        assertTrue(failing.hasError());
        assertSame(oom, failing.getError());
        assertEquals(null, failing.getException());
    }

    @Test
    @STTM("SNAP-4391")
    public void allFailuresAreReportedFirstThrownRestSuppressed() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor(1);   // one thread: tasks run in submission order
        final int n = 5;
        final AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            final int id = i;
            executor.execute(new ThreadRunnable("task " + id) {
                @Override
                public void process() throws Exception {
                    ran.incrementAndGet();
                    if (id % 2 == 0) {
                        throw new IllegalStateException("task " + id);
                    }
                }
            });
        }

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, executor::complete);
        assertEquals(n, ran.get());
        assertEquals("task 0", thrown.getMessage());
        assertEquals(2, thrown.getSuppressed().length);
        assertEquals("task 2", thrown.getSuppressed()[0].getMessage());
        assertEquals("task 4", thrown.getSuppressed()[1].getMessage());
    }

    @Test
    @STTM("SNAP-4391")
    public void sameThrowableFromSeveralTasksIsNotSuppressedIntoItself() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor(1);
        final RuntimeException shared = new RuntimeException("shared");
        for (int i = 0; i < 3; i++) {
            executor.execute(new ThreadRunnable() {
                @Override
                public void process() {
                    throw shared;
                }
            });
        }
        final RuntimeException thrown = assertThrows(RuntimeException.class, executor::complete);
        assertSame(shared, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    @STTM("SNAP-4391")
    public void completeWithoutFailuresDoesNotThrow() throws Exception {
        final ThreadExecutor executor = new ThreadExecutor(3);
        final AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            executor.execute(new ThreadRunnable() {
                @Override
                public void process() {
                    ran.incrementAndGet();
                }
            });
        }
        executor.complete();
        assertEquals(10, ran.get());
    }
}
