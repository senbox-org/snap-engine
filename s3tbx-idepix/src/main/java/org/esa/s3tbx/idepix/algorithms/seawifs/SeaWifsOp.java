package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;

/**
 * The Idepix pixel classification for SeaWiFS products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.Seawifs",
        category = "Optical/Pre-Processing",
        version = "2.2",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for SeaWiFS.")
public class SeaWifsOp extends BasisOp {

    @Parameter(defaultValue = "false",
            label = " Radiance bands (SeaWiFS)",
            description = "Write TOA radiance bands to target product (SeaWiFS).")
    private boolean ocOutputSeawifsRadiance = false;

    @Parameter(defaultValue = "true",
            label = " Reflectance bands (SeaWiFS)",
            description = "Write TOA reflectance bands to target product (SeaWiFS).")
    private boolean ocOutputSeawifsRefl = true;

    @Parameter(defaultValue = "true",
            label = " Geometry bands (SeaWiFS)",
            description = "Write geometry bands to target product (SeaWiFS).")
    private boolean ocOutputGeometry = true;

    @Parameter(defaultValue = "L_", valueSet = {"L_", "Lt_", "rhot_"}, label = " Prefix of input spectral bands (SeaWiFS).",
            description = "Prefix of input radiance or reflectance bands (SeaWiFS)")
    private String ocSeawifsRadianceBandPrefix;

    @Parameter(label = " Product type",
            description = "Defines the product type to use. If the parameter is not set, the product type defined by the input file is used.")
    String productTypeString;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "50", valueSet = {"50", "150"}, label = " Resolution of used land-water mask in m/pixel",
            description = "Resolution in m/pixel")
    private int waterMaskResolution;


    @SourceProduct(alias = "source", label = "Name (SeaWiFS L1b product)", description = "The source product.")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        // todo - take from OccciOp in BEAM Idepix
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SeaWifsOp.class);
        }
    }
}
