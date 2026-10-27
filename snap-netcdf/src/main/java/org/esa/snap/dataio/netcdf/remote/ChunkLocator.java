package org.esa.snap.dataio.netcdf.remote;

import org.esa.snap.core.util.SystemUtils;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf5.DataBTree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

    private ChunkLocator() {
    }

    static List<Range> chunkRanges(Variable variable) {
        List<Range> ranges = new ArrayList<>();
        if (BTREE == null) {
            return ranges;
        }
        try {
            Section want = null;
            Object proxy = variable.getProxyReader();
            if (SLICE_ORIGINAL != null && SLICE_SECTION != null && SLICE_ORIGINAL.getDeclaringClass().isInstance(proxy)) {
                Variable original = (Variable) SLICE_ORIGINAL.get(proxy);
                if (SLICE_ORIGINAL.getDeclaringClass().isInstance(original.getProxyReader())) {
                    return ranges; // nested slices are not supported
                }
                want = (Section) SLICE_SECTION.get(proxy);
                variable = original;
            }
            Object vinfo = variable.getSPobject();
            if (vinfo == null || !BTREE.getDeclaringClass().isInstance(vinfo)) {
                return ranges;
            }
            DataBTree btree = (DataBTree) BTREE.get(vinfo);
            if (btree == null) {
                return ranges;
            }
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
