package org.esa.snap.dataio.netcdf.remote;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for reading NetCDF/HDF5 files over HTTP(S) with byte-range requests instead of downloading them.
 * <p>
 * Tunables (system properties):
 * <ul>
 *     <li>{@code snap.netcdf.remote.blockSizeKB} - size of one cached block, default 256</li>
 *     <li>{@code snap.netcdf.remote.cacheSizeMB} - memory for cached blocks across all remote files, default 512</li>
 *     <li>{@code snap.netcdf.remote.threads} - parallel range requests, default 16</li>
 *     <li>{@code snap.netcdf.remote.readAheadBlocks} - blocks fetched in the background after a cache miss, default 4</li>
 * </ul>
 */
public final class RemoteFileAccess {

    static final int BLOCK_SIZE = Integer.getInteger("snap.netcdf.remote.blockSizeKB", 256) * 1024;
    static final BlockCache CACHE = new BlockCache(Long.getLong("snap.netcdf.remote.cacheSizeMB", 512L) * 1024 * 1024);
    static final int THREADS = Integer.getInteger("snap.netcdf.remote.threads", 16);
    static final int READ_AHEAD = Integer.getInteger("snap.netcdf.remote.readAheadBlocks", 4);

    private static final List<RemoteAuthProvider> AUTH_PROVIDERS = new CopyOnWriteArrayList<>();

    private RemoteFileAccess() {
    }

    /**
     * @return true if the input is an http(s) URL given as a String or URI
     */
    public static boolean isRemoteLocation(Object input) {
        String s = input instanceof URI ? input.toString() : input instanceof String str ? str : null;
        if (s == null) {
            return false;
        }
        String lower = s.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /**
     * @return the file name part of a remote location, without query or fragment
     */
    public static String getFileName(String location) {
        String path = URI.create(location.trim()).getPath();
        if (path == null || path.isEmpty()) {
            return location;
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Opens a remote file for random access. The caller must close it.
     */
    public static ucar.unidata.io.RandomAccessFile open(String location) throws IOException {
        return new HttpRangeRandomAccessFile(location.trim());
    }

    /**
     * Fetches, in parallel, the stored chunks of variables that are about to be read in full.
     * <p>
     * netCDF-Java reads one chunk at a time, which costs a network round trip per chunk on a remote file.
     * Calling this first turns those reads into cache hits. Does nothing for local files, and skips
     * variables whose chunk layout cannot be determined.
     */
    public static void prefetchVariables(ucar.nc2.NetcdfFile ncfile, Iterable<ucar.nc2.Variable> variables) throws IOException {
        if (ncfile == null || !isRemoteLocation(ncfile.getLocation())) {
            return;
        }
        List<ChunkLocator.Range> ranges = new java.util.ArrayList<>();
        for (ucar.nc2.Variable variable : variables) {
            ranges.addAll(ChunkLocator.chunkRanges(variable));
        }
        if (ranges.isEmpty()) {
            return;
        }
        try (HttpRangeRandomAccessFile raf = new HttpRangeRandomAccessFile(ncfile.getLocation().trim())) {
            raf.prefetchRanges(ranges);
        }
    }

    public static void registerAuthProvider(RemoteAuthProvider provider) {
        if (!AUTH_PROVIDERS.contains(provider)) {
            AUTH_PROVIDERS.add(provider);
        }
    }

    public static void unregisterAuthProvider(RemoteAuthProvider provider) {
        AUTH_PROVIDERS.remove(provider);
    }

    /**
     * Drops all cached blocks and resolved URLs of all remote files.
     */
    public static void clearCache() {
        CACHE.clear();
        HttpRangeRandomAccessFile.clearResolved();
    }

    static RemoteAuthProvider findAuthProvider(URI uri) {
        for (RemoteAuthProvider provider : AUTH_PROVIDERS) {
            if (provider.handles(uri)) {
                return provider;
            }
        }
        return null;
    }
}
