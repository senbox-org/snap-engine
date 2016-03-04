package org.esa.s3tbx.insitu.server;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.openide.util.Cancellable;

/**
 * @author Marco Peters
 */
public class InsituServerRunnable implements Runnable, Cancellable {

    private final ProgressHandleMonitor handle;
    private final InsituServer server;
    private final InsituQuery query;
    private Exception exception;
    private InsituResponse response;

    public InsituServerRunnable(InsituServer server, InsituQuery query) {
        this(ProgressHandleMonitor.create("Accessing In-Situ Server"), server, query);
    }

    public InsituServerRunnable(ProgressHandleMonitor handle, InsituServer server, InsituQuery query) {
        this.handle = handle;
        this.server = server;
        this.query = query;
        response = InsituResponse.EMPTY_RESPONSE;
    }

    public ProgressHandleMonitor getHandle() {
        return handle;
    }

    public InsituResponse getResponse() {
        return response;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public final void run() {
        handle.beginTask("Contacting " + server.getName() + " in-situ server", ProgressMonitor.UNKNOWN);
        try {
            response = server.query(query);
        } catch (Exception e) {
            exception = e;
        } finally {
            handle.done();
        }

    }

    @Override
    public boolean cancel() {
        return handle.cancel();
    }
}
