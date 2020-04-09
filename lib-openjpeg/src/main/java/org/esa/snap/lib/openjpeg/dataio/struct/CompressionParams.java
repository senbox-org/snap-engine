package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.List;

public class CompressionParams extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("tile_size_on", "cp_tx0", "cp_ty0", "cp_tdx", "cp_tdy", "cp_disto_alloc", "cp_fixed_alloc",
                    "cp_fixed_quality", "cp_matrice", "cp_comment", "csty", "prog_order", "POC", "numpocs", "tcp_numlayers",
                    "tcp_rates", "tcp_distoratio", "numresolution", "cblockw_init", "cblockh_init", "mode", "irreversible",
                    "roi_compno", "roi_shift", "res_spec", "prcw_init", "prch_init", "infile", "outfile", "index_on",
                    "index", "image_offset_x0", "image_offset_y0", "subsampling_dx", "subsampling_dy", "decod_format",
                    "cod_format", "jpwl_epc_on", "jpwl_hprot_MH", "jpwl_hprot_TPH_tileno", "jpwl_hprot_TPH",
                    "jpwl_pprot_tileno", "jpwl_pprot_packno", "jpwl_pprot", "jpwl_sens_size", "jpwl_sens_addr",
                    "jpwl_sens_range", "jpwl_sens_MH", "jpwl_sens_TPH_tileno", "jpwl_sens_TPH", "cp_cinema",
                    "max_comp_size", "cp_rsiz", "tp_on", "tp_flag", "tcp_mct", "jpip_on", "mct_data", "max_cs_size", "rsiz");
    /**
     * size of tile: tile_size_on = false (not in argument) or = true (in argument)
     * C type : OPJ_BOOL
     */
    public int tile_size_on;
    /** XTOsiz */
    public int cp_tx0;
    /** YTOsiz */
    public int cp_ty0;
    /** XTsiz */
    public int cp_tdx;
    /** YTsiz */
    public int cp_tdy;
    /** allocation by rate/distortion */
    public int cp_disto_alloc;
    /** allocation by fixed layer */
    public int cp_fixed_alloc;
    /** add fixed_quality */
    public int cp_fixed_quality;
    /**
     * fixed layer
     * C type : int*
     */
    public IntByReference cp_matrice;
    /**
     * comment for coding
     * C type : char*
     */
    public Pointer cp_comment;
    /** csty : coding style */
    public int csty;
    /**
     * progression order (default OPJ_LRCP)
     * C type : OPJ_PROG_ORDER
     */
    public int prog_order;
    /**
     * progression order changes
     * C type : opj_poc_t[32]
     */
    public PoC[] POC = new PoC[32];
    /**
     * number of progression order changes (POC), default to 0
     * C type : OPJ_UINT32
     */
    public int numpocs;
    /** number of layers */
    public int tcp_numlayers;
    /**
     * rates of layers - might be subsequently limited by the max_cs_size field
     * C type : float[100]
     */
    public float[] tcp_rates = new float[100];
    /**
     * different psnr for successive layers
     * C type : float[100]
     */
    public float[] tcp_distoratio = new float[100];
    /** number of resolutions */
    public int numresolution;
    /** initial code block width, default to 64 */
    public int cblockw_init;
    /** initial code block height, default to 64 */
    public int cblockh_init;
    /** mode switch (cblk_style) */
    public int mode;
    /** 1 : use the irreversible DWT 9-7, 0 : use lossless compression (default) */
    public int irreversible;
    /** region of interest: affected component in [0..3], -1 means no ROI */
    public int roi_compno;
    /** region of interest: upshift value */
    public int roi_shift;
    /** number of precinct size specifications */
    public int res_spec;
    /**
     * initial precinct width
     * C type : int[33]
     */
    public int[] prcw_init = new int[33];
    /**
     * initial precinct height
     * C type : int[33]
     */
    public int[] prch_init = new int[33];
    /**
     * input file name
     * C type : char[4096]
     */
    public byte[] infile = new byte[4096];
    /**
     * output file name
     * C type : char[4096]
     */
    public byte[] outfile = new byte[4096];
    /** DEPRECATED. Index generation is now handeld with the opj_encode_with_info() function. Set to NULL */
    public int index_on;
    /**
     * DEPRECATED. Index generation is now handeld with the opj_encode_with_info() function. Set to NULL
     * C type : char[4096]
     */
    public byte[] index = new byte[4096];
    /** subimage encoding: origin image offset in x direction */
    public int image_offset_x0;
    /** subimage encoding: origin image offset in y direction */
    public int image_offset_y0;
    /** subsampling value for dx */
    public int subsampling_dx;
    /** subsampling value for dy */
    public int subsampling_dy;
    /** input file format 0: PGX, 1: PxM, 2: BMP 3:TIF */
    public int decod_format;
    /** output file format 0: J2K, 1: JP2, 2: JPT */
    public int cod_format;
    /**
     * enables writing of EPC in MH, thus activating JPWL
     * C type : OPJ_BOOL
     */
    public int jpwl_epc_on;
    /** error protection method for MH (0,1,16,32,37-128) */
    public int jpwl_hprot_MH;
    /**
     * tile number of header protection specification (>=0)
     * C type : int[16]
     */
    public int[] jpwl_hprot_TPH_tileno = new int[16];
    /**
     * error protection methods for TPHs (0,1,16,32,37-128)
     * C type : int[16]
     */
    public int[] jpwl_hprot_TPH = new int[16];
    /**
     * tile number of packet protection specification (>=0)
     * C type : int[16]
     */
    public int[] jpwl_pprot_tileno = new int[16];
    /**
     * packet number of packet protection specification (>=0)
     * C type : int[16]
     */
    public int[] jpwl_pprot_packno = new int[16];
    /**
     * error protection methods for packets (0,1,16,32,37-128)
     * C type : int[16]
     */
    public int[] jpwl_pprot = new int[16];
    /** enables writing of ESD, (0=no/1/2 bytes) */
    public int jpwl_sens_size;
    /** sensitivity addressing size (0=auto/2/4 bytes) */
    public int jpwl_sens_addr;
    /** sensitivity range (0-3) */
    public int jpwl_sens_range;
    /** sensitivity method for MH (-1=no,0-7) */
    public int jpwl_sens_MH;
    /**
     * tile number of sensitivity specification (>=0)
     * C type : int[16]
     */
    public int[] jpwl_sens_TPH_tileno = new int[16];
    /**
     * sensitivity methods for TPHs (-1=no,0-7)
     * C type : int[16]
     */
    public int[] jpwl_sens_TPH = new int[16];
    /**
     * DEPRECATED: use RSIZ, OPJ_PROFILE_* and MAX_COMP_SIZE instead
     * Digital Cinema compliance 0-not compliant, 1-compliant
     * C type : OPJ_CINEMA_MODE
     */
    public int cp_cinema;
    /**
     * Maximum size (in bytes) for each component.
     * If == 0, component size limitation is not considered
     */
    public int max_comp_size;
    /**
     * DEPRECATED: use RSIZ, OPJ_PROFILE_* and OPJ_EXTENSION_* instead
     * Profile name
     * C type : OPJ_RSIZ_CAPABILITIES
     */
    public int cp_rsiz;
    /** Tile part generation */
    public byte tp_on;
    /** Flag for Tile part generation */
    public byte tp_flag;
    /** MCT (multiple component transform) */
    public char tcp_mct;
    /**
     * Enable JPIP indexing
     * C type : OPJ_BOOL
     */
    public int jpip_on;
    /**
     * Naive implementation of MCT restricted to a single reversible array based 
     * encoding without offset concerning all the components.
     * C type : void*
     */
    public Pointer mct_data;
    /**
     * Maximum size (in bytes) for the whole codestream.
     * If == 0, codestream size limitation is not considered
     * If it does not comply with tcp_rates, max_cs_size prevails
     * and a warning is issued.
     */
    public int max_cs_size;
    /**
     * RSIZ value
     * To be used to combine OPJ_PROFILE_*, OPJ_EXTENSION_* and (sub)levels values.
     * C type : OPJ_UINT16
     */
    public short rsiz;

    public CompressionParams() {
        super();
    }

    public CompressionParams(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends CompressionParams implements Structure.ByReference {
    }

    public static class ByValue extends CompressionParams implements Structure.ByValue {
    }
}
