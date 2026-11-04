package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;
import java.awt.image.DataBuffer;

/**
 * @author Tonio Fincke
 */
class DataAccessorFactory {

    static LongDataAccessor createLongDataAccessor(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        return switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE -> new LongDataAccessor.Byte(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_SHORT -> new LongDataAccessor.Short(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_USHORT -> new LongDataAccessor.UShort(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_INT -> new LongDataAccessor.Int(srcAccessor, dstAccessor, noDataValue);
            default -> throw new IllegalArgumentException("Datatype not supported");
        };
    }

    static DoubleDataAccessor createDoubleDataAccessor(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        return switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_FLOAT -> new DoubleDataAccessor.Float(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_DOUBLE -> new DoubleDataAccessor.Double(srcAccessor, dstAccessor, noDataValue);
            default -> throw new IllegalArgumentException("Datatype not supported");
        };
    }
}
