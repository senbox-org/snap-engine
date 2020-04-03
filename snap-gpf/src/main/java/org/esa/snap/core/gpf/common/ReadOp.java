/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.subset.AbstractSubsetRegion;
import org.esa.snap.core.subset.GeometrySubsetRegion;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.converters.RectangleConverter;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Reads the specified file as product. This operator may serve as a source node in processing graphs,
 * especially if multiple data products need to be read in.
 * <p>
 * Here is a sample of how the <code>Read</code> operator can be integrated as a node within a processing graph:
 * <pre>
 *    &lt;node id="readNode"&gt;
 *        &lt;operator&gt;Read&lt;/operator&gt;
 *        &lt;parameters&gt;
 *            &lt;file&gt;/eodata/SST.nc&lt;/file&gt;
 *            &lt;formatName&gt;GeoTIFF&lt;/formatName&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "Read",
        category = "Input-Output",
        version = "1.2",
        authors = "Marco Zuehlke, Norman Fomferra",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Reads a data product from a given file location.")
public class ReadOp extends Operator {

    @Parameter(description = "The file from which the data product is read.", notNull = true, notEmpty = true)
    private File file;

    @Parameter(description = "An (optional) format name.", notNull = false, notEmpty = true)
    private String formatName;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", label = "Source Bands")
    private String[] bandNames;

    @Parameter(description = "The list of source masks.", alias = "sourceMasks", label = "Source Masks")
    private String[] maskNames;

    @Parameter(converter = RectangleConverter.class,
            description = "The subset region in pixel coordinates.\n" +
                    "Use the following format: <x>,<y>,<width>,<height>\n" +
                    "If not given, the entire scene is used. The 'geoRegion' parameter has precedence over this parameter.")
    private Rectangle pixelRegion;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The subset region in geographical coordinates using WKT-format,\n" +
                    "e.g. POLYGON((<lon1> <lat1>, <lon2> <lat2>, ..., <lon1> <lat1>))\n" +
                    "(make sure to quote the option due to spaces in <geometry>).\n" +
                    "If not given, the entire scene is used.")
    private Geometry geometryRegion;

    /**
     * The default value for copy metadata is true, to copy them if the flag is not specified.
     */
    @Parameter(defaultValue = "true", description = "Whether to copy the metadata of the source product.")
    private boolean copyMetadata;

    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        if (this.file == null) {
            throw new OperatorException("The 'file' parameter is not set");
        }
        if (!this.file.exists()) {
            throw new OperatorException(String.format("Specified 'file' [%s] does not exist.", this.file));
        }
        if (this.pixelRegion != null && this.geometryRegion != null) {
            throw new OperatorException("Both types of region are specified: pixel and geometry. At most one must be specified.");
        }
        boolean hasBandNames = (this.bandNames != null && this.bandNames.length > 0);
        boolean hasMaskNames = (this.maskNames != null && this.maskNames.length > 0);
        ProductSubsetDef subsetDef = null;
        if (hasBandNames || hasMaskNames || this.pixelRegion != null || this.geometryRegion != null || !this.copyMetadata) {
            subsetDef = new ProductSubsetDef();
            subsetDef.setIgnoreMetadata(!this.copyMetadata);
            AbstractSubsetRegion subsetRegion = null;
            if (this.geometryRegion != null) {
                subsetRegion = new GeometrySubsetRegion(this.geometryRegion, 0);
            } else if (this.pixelRegion != null) {
                subsetRegion = new PixelSubsetRegion(this.pixelRegion, 0);
            }
            subsetDef.setSubsetRegion(subsetRegion);
            if (hasBandNames) {
                subsetDef.addNodeNames(this.bandNames);
            }
            if (hasMaskNames) {
                subsetDef.addNodeNames(this.maskNames);
            }
        }

        try {
            Product openedProduct = getOpenedProduct();
            if (openedProduct != null && subsetDef == null) {
                this.targetProduct = new Product(openedProduct.getName(), openedProduct.getProductType(), openedProduct.getSceneRasterWidth(), openedProduct.getSceneRasterHeight());
                for (Band srcband : openedProduct.getBands()) {
                    if (this.targetProduct.getBand(srcband.getName()) != null) {
                        continue;
                    }
                    if (srcband instanceof VirtualBand) {
                        ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcband, srcband.getName());
                    } else {
                        ProductUtils.copyBand(srcband.getName(), openedProduct, this.targetProduct, true);
                    }
                }
                ProductUtils.copyProductNodes(openedProduct, this.targetProduct);
                this.targetProduct.setFileLocation(openedProduct.getFileLocation());
            } else {
                ProductReader productReader;
                if (this.formatName != null && !this.formatName.trim().isEmpty()) {
                    productReader = ProductIO.getProductReader(this.formatName);
                    if (productReader == null) {
                        throw new OperatorException("No product reader found for format '" + this.formatName + "'.");
                    }
                } else {
                    productReader = ProductIO.getProductReaderForInput(this.file);
                    if (productReader == null) {
                        throw new OperatorException("No product reader found for file '" + this.file.getAbsolutePath() + "'.");
                    }
                }
                this.targetProduct = productReader.readProductNodes(this.file, subsetDef);
                this.targetProduct.setFileLocation(this.file);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        ProductData dataBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            this.targetProduct.getProductReader().readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width, rectangle.height, dataBuffer, pm);
            targetTile.setRawSamples(dataBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private Product getOpenedProduct() {
        Product[] openedProducts = getProductManager().getProducts();
        for (Product openedProduct : openedProducts) {
            if (this.file.equals(openedProduct.getFileLocation())) {
                return openedProduct;
            }
        }
        return null;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ReadOp.class);
        }
    }
}
