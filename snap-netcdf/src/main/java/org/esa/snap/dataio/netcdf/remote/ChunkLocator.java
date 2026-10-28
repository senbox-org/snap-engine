package org.esa.snap.dataio.netcdf.remote;

import org.esa.snap.core.util.SystemUtils;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf5.DataBTree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Finds where the stored chunks of an HDF5 variable lie in the file, so they can be fetched in parallel
 * before netCDF-Java reads them one at a time.
 * <p>
 * netCDF-Java keeps a variable's chunk index ({@link DataBTree}) in a package-private field of its
 * {@code H5header.Vinfo}; it is read reflectively. If that fails (another netCDF-Java version, a non-HDF5
 * file, an unchunked variable) no ranges are returned and reading simply proceeds without the prefetch.
 * <p>
 * A variable made with {@code Variable.slice} (one level) yields only the chunks that hold the slice.
 */
final class ChunkLocator {

    record Range(long start, long length) {
    }

    private static final Field BTREE = findField("ucar.nc2.iosp.hdf5.H5header$Vinfo", "btree");
    private static final Field SLICE_ORIGINAL = findField("ucar.nc2.SliceReader", "orgClient");
    private static final Field SLICE_SECTION = findField("ucar.nc2.SliceReader", "slice");
    private static final Field ROOT = findField("ucar.nc2.iosp.hdf5.DataBTree", "rootNodeAddress");
    private static final Field NDIM = findField("ucar.nc2.iosp.hdf5.DataBTree", "ndimStorage");

    private ChunkLocator() {
    }

    /** Bytes fetched per index node: covers nodes of up to 64 entries for variables of rank 4 or less. */
    private static final int NODE_BYTES = 8192;
    private static final int MAX_INDEX_DEPTH = 16;

    /**
     * Loads the chunk indexes of variables into the block cache one tree level at a time, fetching all nodes
     * of a level, across all the variables, in parallel. Walking an index afterwards ({@link #chunkRanges})
     * then needs no round trips; without this, every node costs one. Reads through {@code raf}, not the
     * file's own handle, so it can run while other threads read from the file.
     * <p>
     * Parses HDF5 version 1 B-tree nodes ("TREE", node type 1). Skips anything unexpected:
     * nodes it has not loaded are simply read on demand later.
     */
    static void warmIndexes(Iterable<Variable> variables, HttpRangeRandomAccessFile raf) {
        if (ROOT == null || NDIM == null) {
            return;
        }
        // node address -> key size of the index it belongs to
        Map<Long, Integer> level = new LinkedHashMap<>();
        for (Variable variable : variables) {
            DataBTree btree = btreeOf(variable);
            if (btree != null) {
                try {
                    level.put(ROOT.getLong(btree), 8 + 8 * NDIM.getInt(btree));
                } catch (Exception | LinkageError e) {
                    SystemUtils.LOG.log(Level.FINE, "Cannot preload chunk index of " + variable.getFullName(), e);
                }
            }
        }
        try {
            for (int depth = 0; depth < MAX_INDEX_DEPTH && !level.isEmpty(); depth++) {
                List<Range> nodes = new ArrayList<>();
                for (long address : level.keySet()) {
                    if (address > 0 && address < raf.length()) {
                        nodes.add(new Range(address, Math.min(NODE_BYTES, raf.length() - address)));
                    }
                }
                raf.prefetchRanges(nodes);
                Map<Long, Integer> next = new LinkedHashMap<>();
                for (Map.Entry<Long, Integer> node : level.entrySet()) {
                    List<Long> children = new ArrayList<>();
                    if (readChildNodes(raf, node.getKey(), node.getValue(), children)) {
                        for (long child : children) {
                            next.put(child, node.getValue());
                        }
                    }
                }
                level = next;
            }
        } catch (Exception | LinkageError e) {
            SystemUtils.LOG.log(Level.FINE, "Cannot preload chunk indexes of " + raf.getLocation(), e);
        }
    }

    /**
     * Reads one index node and adds its child node addresses (none for a leaf, whose children are chunks).
     *
     * @return false if the bytes are not a chunk index node
     */
    private static boolean readChildNodes(HttpRangeRandomAccessFile raf, long address, int keyBytes, List<Long> children)
            throws java.io.IOException {
        if (address <= 0 || address + 24 > raf.length()) {
            return false;
        }
        raf.order(ucar.unidata.io.RandomAccessFile.LITTLE_ENDIAN);
        raf.seek(address);
        byte[] signature = new byte[4];
        raf.readFully(signature);
        if (signature[0] != 'T' || signature[1] != 'R' || signature[2] != 'E' || signature[3] != 'E') {
            return false;
        }
        int nodeType = raf.readByte();
        int nodeLevel = raf.readByte();
        int entries = raf.readShort() & 0xFFFF;
        if (nodeType != 1) {
            return false;
        }
        if (nodeLevel == 0) {
            return true;
        }
        raf.skipBytes(16); // left and right sibling addresses
        for (int i = 0; i < entries; i++) {
            raf.skipBytes(keyBytes);
            long child = raf.readLong();
            if (child <= 0 || child >= raf.length()) {
                return false;
            }
            children.add(child);
        }
        return true;
    }

    static List<Range> chunkRanges(Variable variable) {
        List<Range> ranges = new ArrayList<>();
        try {
            Section want = sliceSection(variable);
            DataBTree btree = btreeOf(variable);
            if (btree == null) {
                return ranges;
            }
            Object vinfo = original(variable).getSPobject();
            // the "filter" iterator returns every chunk; keep those that intersect the wanted section
            int[] chunkShape = ((ucar.nc2.iosp.hdf5.H5header.Vinfo) vinfo).getChunking();
            DataBTree.DataChunkIterator it = btree.getDataChunkIteratorFilter(null);
            while (it.hasNext()) {
                DataBTree.DataChunk chunk = it.next();
                if (chunk.filePos >= 0 && chunk.size > 0 && intersects(chunk.offset, chunkShape, want)) {
                    ranges.add(new Range(chunk.filePos, chunk.size));
                }
            }
        } catch (Exception | LinkageError e) {
            SystemUtils.LOG.log(Level.FINE, "Cannot locate chunks of " + variable.getFullName(), e);
            ranges.clear();
        }
        return ranges;
    }

    /**
     * @return the variable a slice was made from, the variable itself if it is not a slice,
     * or null for a slice of a slice (not supported)
     */
    private static Variable original(Variable variable) throws IllegalAccessException {
        Object proxy = variable.getProxyReader();
        if (SLICE_ORIGINAL != null && SLICE_ORIGINAL.getDeclaringClass().isInstance(proxy)) {
            Variable original = (Variable) SLICE_ORIGINAL.get(proxy);
            return SLICE_ORIGINAL.getDeclaringClass().isInstance(original.getProxyReader()) ? null : original;
        }
        return variable;
    }

    private static Section sliceSection(Variable variable) throws IllegalAccessException {
        Object proxy = variable.getProxyReader();
        if (SLICE_SECTION != null && SLICE_SECTION.getDeclaringClass().isInstance(proxy)) {
            return (Section) SLICE_SECTION.get(proxy);
        }
        return null;
    }

    private static DataBTree btreeOf(Variable variable) {
        if (BTREE == null) {
            return null;
        }
        try {
            Variable original = original(variable);
            Object vinfo = original == null ? null : original.getSPobject();
            if (vinfo == null || !BTREE.getDeclaringClass().isInstance(vinfo)) {
                return null;
            }
            return (DataBTree) BTREE.get(vinfo);
        } catch (Exception | LinkageError e) {
            SystemUtils.LOG.log(Level.FINE, "Cannot find chunk index of " + variable.getFullName(), e);
            return null;
        }
    }

    private static boolean intersects(int[] chunkStart, int[] chunkShape, Section want) {
        if (want == null) {
            return true;
        }
        for (int d = 0; d < want.getRank(); d++) {
            ucar.ma2.Range r = want.getRange(d);
            int start = chunkStart[d];
            int end = start + chunkShape[d] - 1;
            if (end < r.first() || start > r.last()) {
                return false;
            }
        }
        return true;
    }

    private static Field findField(String className, String name) {
        try {
            Field field = Class.forName(className).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception | LinkageError e) {
            SystemUtils.LOG.log(Level.FINE, "HDF5 chunk prefetch unavailable", e);
            return null;
        }
    }
}
