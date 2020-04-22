package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   openjpeg.h
 * C structure:     opj_image_comptparm
 */
public class ImageComponentParams extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("dx", "dy", "w", "h", "x0", "y0", "prec", "bpp", "sgnd");

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
    public int prec;  /* OPJ_UINT32 */
    /**
     * image depth in bits
     */
    public int bpp;  /* OPJ_UINT32 */
    /**
     * signed (1) / unsigned (0)
     */
    public int sgnd;  /* OPJ_UINT32 */

    public ImageComponentParams() {
        super();
    }

    public ImageComponentParams(Pointer peer) {
        super(peer);
    }

    public ImageComponentParams(int dx, int dy, int w, int h, int x0, int y0, int prec, int bpp, int sgnd) {
        super();
        this.dx = dx;
        this.dy = dy;
        this.w = w;
        this.h = h;
        this.x0 = x0;
        this.y0 = y0;
        this.prec = prec;
        this.bpp = bpp;
        this.sgnd = sgnd;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends ImageComponentParams implements Structure.ByReference {
    }

    public static class ByValue extends ImageComponentParams implements Structure.ByValue {
    }
}
