package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;


public class CachedSubsamplingReader {


    private CachedSubsamplingReader() {}

    /**
     * Reads a 2D band region from the cache with subsampling support.
     * <p>
     * When sourceStepX == 1 and sourceStepY == 1 the read goes directly into destBuffer
     * without any temporary allocation (fast path).
     * <p>
     * When either step is > 1 the full source rectangle (sourceWidth * sourceHeight) is cached
     * and read into a temporary buffer first. the subsampled pixels are then copied into destBuffer.
     *
     * @param cache          the ProductCache to read from
     * @param bandName       band / variable name passed to the cache
     * @param dataType       ProductData type constant used to allocate the temporary buffer
     * @param sourceOffsetX  x-origin of the source region (in source pixel coordinates)
     * @param sourceOffsetY  y-origin of the source region (in source pixel coordinates)
     * @param sourceWidth    width  of the full source region (before subsampling)
     * @param sourceHeight   height of the full source region (before subsampling)
     * @param sourceStepX    subsampling step in x direction (1 = no subsampling)
     * @param sourceStepY    subsampling step in y direction (1 = no subsampling)
     * @param destWidth      width  of the destination buffer
     * @param destHeight     height of the destination buffer
     * @param destBuffer     target ProductData to write into
     * @throws IOException   if the cache read fails
     */
    public static void read(ProductCache cache,
                            String bandName,
                            int dataType,
                            int sourceOffsetX, int sourceOffsetY,
                            int sourceWidth, int sourceHeight,
                            int sourceStepX, int sourceStepY,
                            int destWidth, int destHeight,
                            ProductData destBuffer) throws IOException {

        final int[] offsets = {sourceOffsetY, sourceOffsetX};

        if (sourceStepX == 1 && sourceStepY == 1) {
            final int[] shapes = {destHeight, destWidth};
            cache.read(bandName, offsets, shapes, new DataBuffer(destBuffer, offsets, shapes));
        } else {
            final int[] shapes = {sourceHeight, sourceWidth};
            final ProductData tempData = ProductData.createInstance(dataType, sourceWidth * sourceHeight);

            cache.read(bandName, offsets, shapes, new DataBuffer(tempData, offsets, shapes));

            for (int dest_yy = 0; dest_yy < destHeight; dest_yy++) {
                for (int dest_xx = 0; dest_xx < destWidth; dest_xx++) {
                    final int srcIdx = (dest_yy * sourceStepY) * sourceWidth + (dest_xx * sourceStepX);
                    final int destIdx = dest_yy * destWidth + dest_xx;

                    destBuffer.setElemDoubleAt(destIdx, tempData.getElemDoubleAt(srcIdx));
                }
            }
        }
    }
}
