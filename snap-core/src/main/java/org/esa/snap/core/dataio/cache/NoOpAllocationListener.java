package org.esa.snap.core.dataio.cache;

class NoOpAllocationListener implements AllocationListener {

    @Override
    public long allocated(long numBytes) {
        return 0;
    }
}
