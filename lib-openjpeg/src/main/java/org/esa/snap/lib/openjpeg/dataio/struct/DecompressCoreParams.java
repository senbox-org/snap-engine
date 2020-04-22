package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   openjpeg.h
 * C structure:     opj_dparameters
 *
 * Decompression parameters
 */
public class DecompressCoreParams extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("cp_reduce", "cp_layer", "infile", "outfile", "decod_format", "cod_format", "DA_x0", "DA_x1",
                    "DA_y0", "DA_y1", "m_verbose", "tile_index", "nb_tile_to_decode", "jpwl_correct", "jpwl_exp_comps",
                    "jpwl_max_tiles", "flags");

    /**
     * Set the number of highest resolution levels to be discarded. 
     * The image resolution is effectively divided by 2 to the power of the number of discarded levels. 
     * The reduce factor is limited by the smallest total number of decomposition levels among tiles.
     * if != 0, then original dimension divided by 2^(reduce); 
     * if == 0 or not used, image is decoded to the full resolution
     */
    public int cp_reduce; /* OPJ_UINT32 */
    /**
     * Set the maximum number of quality layers to decode. 
     * If there are less quality layers than the specified number, all the quality layers are decoded.
     * if != 0, then only the first "layer" layers are decoded; 
     * if == 0 or not used, all the quality layers are decoded
     */
    public int cp_layer; /* OPJ_UINT32 */
    /**
     * input file name
     */
    public byte[] infile = new byte[4096]; /* char[4096] */
    /**
     * output file name
     */
    public byte[] outfile = new byte[4096];  /* char[4096 */
    /**
     * input file format 0: J2K, 1: JP2, 2: JPT
     * */
    public int decod_format; /* OPJ_UINT32 */
    /**
     * output file format 0: PGX, 1: PxM, 2: BMP
     * */
    public int cod_format; /* OPJ_UINT32 */
    /**
     * Decoding area left boundary
     */
    public int DA_x0; /* OPJ_UINT32 */
    /**
     * Decoding area right boundary
     */
    public int DA_x1; /* OPJ_UINT32 */
    /**
     * Decoding area up boundary
     */
    public int DA_y0; /* OPJ_UINT32 */
    /**
     * Decoding area bottom boundary
     */
    public int DA_y1; /* OPJ_UINT32 */
    /**
     * Verbose mode
     */
    public int m_verbose; /* OPJ_BOOL */
    /**
     * tile number ot the decoded tile
     */
    public int tile_index; /* OPJ_UINT32 */
    /**
     * Nb of tile to decode
     */
    public int nb_tile_to_decode; /* OPJ_UINT32 */
    /**
     * activates the JPWL correction capabilities
     */
    public int jpwl_correct; /* OPJ_BOOL */
    /**
     * expected number of components
     * */
    public int jpwl_exp_comps; /* OPJ_UINT32 */
    /**
     * maximum number of tiles
     * */
    public int jpwl_max_tiles; /* OPJ_UINT32 */

    public int flags; /* unsigned int */

    public DecompressCoreParams() {
        super();
    }

    public DecompressCoreParams(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends DecompressCoreParams implements Structure.ByReference {
    }

    public static class ByValue extends DecompressCoreParams implements Structure.ByValue {
    }
}
