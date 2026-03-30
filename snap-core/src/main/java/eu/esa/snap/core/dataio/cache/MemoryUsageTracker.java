package eu.esa.snap.core.dataio.cache;

public interface MemoryUsageTracker {

    void allocated(long numBytes);
    void released(long numBytes);
}
