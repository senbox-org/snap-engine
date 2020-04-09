package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByteByReference;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   openjpeg.h
 * C structure:     opj_image_t
 */
public class Image extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("x0", "y0", "x1", "y1", "numcomps", "color_space", "comps", "icc_profile_buf", "icc_profile_len");

    /**
     * XOsiz: horizontal offset from the origin of the reference grid to the left side of the image area
     */
    public int x0; /* OPJ_UINT32 */
    /**
     * YOsiz: vertical offset from the origin of the reference grid to the top side of the image area
     */
    public int y0; /* OPJ_UINT32 */
    /**
     * Xsiz: width of the reference grid
     */
    public int x1;/* OPJ_UINT32 */
    /**
     * Ysiz: height of the reference grid
     */
    public int y1; /* OPJ_UINT32 */
    /**
     * number of components in the image
     */
    public int numcomps; /* OPJ_UINT32 */
    /**
     * color space: sRGB, Greyscale or YUV
     */
    public int color_space; /* OPJ_COLOR_SPACE */
    /**
     * image components
     */
    public ImageComponent.ByReference comps; /* opj_image_comp_t* */
    /**
     * 'restricted' ICC profile
     */
    public ByteByReference icc_profile_buf; /* OPJ_BYTE* */
    /**
     * size of ICC profile
     */
    public int icc_profile_len; /* OPJ_UINT32 */

    public Image() {
        super();
        comps = new ImageComponent.ByReference();
    }

    public Image(Pointer peer) {
        super(peer);
    }

    public Image(int x0, int y0, int x1, int y1, int numcomps, int color_space, ImageComponent.ByReference comps, ByteByReference icc_profile_buf, int icc_profile_len) {
        super();
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.numcomps = numcomps;
        this.color_space = color_space;
        this.comps = comps;
        this.icc_profile_buf = icc_profile_buf;
        this.icc_profile_len = icc_profile_len;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends Image implements Structure.ByReference {
    }

    public static class ByValue extends Image implements Structure.ByValue {
    }
}
