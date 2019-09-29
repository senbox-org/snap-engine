package org.esa.snap.remote.execution.machines;

/**
 * Created by jcoravu on 29/3/2019.
 */
public class RemoteMachineConnectionResult {

    private final boolean isRemoteMachineAvailable;
    private final boolean isGPTApplicationAvailable;

    public RemoteMachineConnectionResult(boolean isRemoteMachineAvailable, boolean isGPTApplicationAvailable) {
        this.isRemoteMachineAvailable = isRemoteMachineAvailable;
        this.isGPTApplicationAvailable = isGPTApplicationAvailable;
    }

    public boolean isRemoteMachineAvailable() {
        return isRemoteMachineAvailable;
    }

    public boolean isGPTApplicationAvailable() {
        return isGPTApplicationAvailable;
    }
}
