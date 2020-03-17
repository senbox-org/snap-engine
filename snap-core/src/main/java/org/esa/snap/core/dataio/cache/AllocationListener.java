package org.esa.snap.core.dataio.cache;

interface AllocationListener {

    long allocated(long numBytes);
}
