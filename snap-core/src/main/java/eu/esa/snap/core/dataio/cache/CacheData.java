package eu.esa.snap.core.dataio.cache;

interface CacheData {

    int getSizeInBytes();

    long release(long bytesToRelease);
}
