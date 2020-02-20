package org.esa.snap.core.util;

import org.junit.Test;

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
}
