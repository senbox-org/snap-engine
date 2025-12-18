package eu.esa.snap.core.dataio.cache;

interface CacheData {

    int getSizeInBytes();

    static boolean intersectingRange(int testMin, int testMax, int min, int max) {
        return testMax >= min && testMax <= max || testMin >= min && testMin <= max || testMin <= min && testMax >= max;
    }
}
