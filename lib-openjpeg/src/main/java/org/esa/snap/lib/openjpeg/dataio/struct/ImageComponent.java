package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   openjpeg.h
 * C structure:     opj_image_comp_t
 */
public class ImageComponent extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("dx", "dy", "w", "h", "x0", "y0", "prec", "bpp", "sgnd", "resno_decoded", "factor", "data", "alpha");

    /**
     * XRsiz: horizontal separation of a sample of ith component with respect to the reference grid
     */
    public int dx; /* OPJ_UINT32 */
    /**
     * YRsiz: vertical separation of a sample of ith component with respect to the reference grid
     */
    public int dy; /* OPJ_UINT32 */
    /**
     * data width
     */
    public int w; /* OPJ_UINT32 */
    /**
     * data height
     */
    public int h; /* OPJ_UINT32 */
    /**
     * x component offset compared to the whole image
     */
    public int x0; /* OPJ_UINT32 */
    /**
     * y component offset compared to the whole image
     */
    public int y0; /* OPJ_UINT32 */
    /**
     * precision
     */
    public int prec; /* OPJ_UINT32 */
    /**
     * image depth in bits
     */
    public int bpp; /* OPJ_UINT32 */
    /**
     * signed (1) / unsigned (0)
     */
    public int sgnd; /* OPJ_UINT32 */
    /**
     * number of decoded resolution
     */
    public int resno_decoded; /* OPJ_UINT32 */
    /**
     * number of division by 2 of the out image compared to the original size of image
     */
    public int factor; /* OPJ_UINT32 */
    /**
     * image component data
     */
    public IntByReference data; /* OPJ_INT32* */
    /**
     * alpha channel
     */
    public short alpha; /* OPJ_UINT16 */

    public ImageComponent() {
        super();
        data = new IntByReference();
    }

    public ImageComponent(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends ImageComponent implements Structure.ByReference {
    }

    public static class ByValue extends ImageComponent implements Structure.ByValue {
    }
}
