package eu.esa.snap.core.dataio.cache;

public class TestMemoryUsageTracker implements MemoryUsageTracker {

    private long allocatedBytes;

    public TestMemoryUsageTracker() {
        allocatedBytes = 0;
    }

    @Override
    public void allocate(long numBytes) {
        allocatedBytes += numBytes;
    }

    @Override
    public void free(long numBytes) {
        allocatedBytes -= numBytes;
    }

    public long getAllocatedBytes() {
        return allocatedBytes;
    }
}
