package eu.esa.snap.core.dataio.cache;

public interface MemoryUsageTracker {

    void allocate(long numBytes);
    void free(long numBytes);
}
