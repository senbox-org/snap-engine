/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetByPolygon;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.subset.SubsetRegionInfo;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.converters.RectangleConverter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * This operator is used to create either spatial and/or spectral subsets of a data product.
 * Spatial subset may be given by pixel positions (parameter {@code region})
 * or a geographical polygon (parameter {@code geoRegion}). Subsets of band and tie-point grid
 * are given by name lists (parameters {@code bandNames} and  {@code tiePointGridNames}).
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Florian Douziech
 * @since BEAM 4.9
 */
@OperatorMetadata(alias = "Subset",
        category = "Raster",
        authors = "SNAP Developers",
        version = "1.3",
        copyright = "(c) 2011 by Brockmann Consult",
        description = "Create a spatial and/or spectral subset of a data product.")
public class SubsetOp extends Operator {

    @SourceProduct(alias = "source", description = "The source product to create the subset from.")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] bandNames;

    @Parameter(description = "The list of tie-point grid names.", alias = "tiePointGrids",
            rasterDataNodeType = TiePointGrid.class, label = "Tie-Point Grids")
    private String[] tiePointGridNames;

    @Parameter(converter = RectangleConverter.class,
            description = "The subset region in pixel coordinates.\n" +
                    "Use the following format: {x},{y},{width},{height}\n" +
                    "If not given, the entire scene is used. The 'geoRegion' parameter has precedence over this parameter.")
    private Rectangle region = null;

    @Parameter(description = "The band used to indicate the pixel coordinates.", alias = "referenceBand",
            rasterDataNodeType = Band.class, label = "Reference Band")
    private String referenceBand = null;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The subset region in geographical coordinates using WKT-format,\n" +
                    "e.g. POLYGON(({lon1} {lat1}, {lon2} {lat2}, ..., {lon1} {lat1}))\n" +
                    "(make sure to quote the option due to spaces in {geometry}).\n" +
                    "If not given, the entire scene is used.")
    private Geometry geoRegion;
    @Parameter(defaultValue = "1",
            description = "The pixel sub-sampling step in X (horizontal image direction)")
    private int subSamplingX;
    @Parameter(defaultValue = "1",
            description = "The pixel sub-sampling step in Y (vertical image direction)")
    private int subSamplingY;
    @Parameter(defaultValue = "false",
            description = "Forces the operator to extend the subset region to the full swath.")
    private boolean fullSwath;
    @Parameter(description = "The file from which the polygon is read.")
    private File vectorFile;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The subset region in geographical coordinates using WKT-format,\n" +
                    "e.g. POLYGON(({lon1} {lat1}, {lon2} {lat2}, ..., {lon1} {lat1}))\n" +
                    "If not given, the geometryRegion or pixelRegion is used.")
    private Polygon polygonRegion;

    @Parameter(defaultValue = "false", description = "Whether to copy the metadata of the source product.")
    private boolean copyMetadata;

    private transient ProductReader subsetReader;

    private final ProductSubsetByPolygon productSubsetByPolygon = new ProductSubsetByPolygon();

    public SubsetOp() {
        this.subSamplingX = 1;
        this.subSamplingY = 1;
        this.fullSwath = false;
        this.copyMetadata = false;
    }

    public String[] getTiePointGridNames() {
        return tiePointGridNames != null ? tiePointGridNames.clone() : null;
    }

    public void setTiePointGridNames(String[] tiePointGridNames) {
        this.tiePointGridNames = tiePointGridNames != null ? tiePointGridNames.clone() : null;
    }

    public String[] getBandNames() {
        return bandNames != null ? bandNames.clone() : null;
    }

    public void setBandNames(String[] bandNames) {
        this.bandNames = bandNames != null ? bandNames.clone() : null;
    }

    public void setCopyMetadata(boolean copyMetadata) {
        this.copyMetadata = copyMetadata;
    }

    public Rectangle getRegion() {
        return region != null ? new Rectangle(region) : null;
    }

    public void setRegion(Rectangle region) {
        this.region = region != null ? new Rectangle(region) : null;
    }

    public void setSubSamplingX(int subSamplingX) {
        this.subSamplingX = subSamplingX;
    }

    public void setSubSamplingY(int subSamplingY) {
        this.subSamplingY = subSamplingY;
    }

    public Geometry getGeoRegion() {
        return geoRegion;
    }

    public void setGeoRegion(Geometry geoRegion) {
        this.geoRegion = geoRegion;
    }


    @Override
    public void initialize() throws OperatorException {
        boolean isMultisize = sourceProduct.isMultiSize();

        subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        if (tiePointGridNames != null) {
            subsetDef.addNodeNames(tiePointGridNames);
        }else{
            subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        }

        if (bandNames != null && bandNames.length > 0) {
            subsetDef.addNodeNames(bandNames);
        } else {
            subsetDef.addNodeNames(sourceProduct.getBandNames());
        }
        String[] nodeNames = subsetDef.getNodeNames();
        if (nodeNames != null) {
            final ArrayList<String> referencedNodeNames = new ArrayList<>();
            for (String nodeName : nodeNames) {
                collectReferencedRasters(nodeName, referencedNodeNames);
            }
            subsetDef.addNodeNames(referencedNodeNames.toArray(new String[0]));
        }

        try {
            if (this.vectorFile != null || this.polygonRegion != null) {
                final MetadataInspector.Metadata productMetadata = new MetadataInspector.Metadata(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
                productMetadata.setGeoCoding(sourceProduct.getSceneGeoCoding());
                if (this.polygonRegion != null) {
                    this.productSubsetByPolygon.loadPolygonFromWKTString(this.polygonRegion.toText(), false, productMetadata, ProgressMonitor.NULL);
                } else {
                    this.productSubsetByPolygon.loadPolygonFromVectorFile(this.vectorFile, productMetadata, ProgressMonitor.NULL);
                }
                if (referenceBand != null) {
                    final GeoCoding referenceBandGeocoding = sourceProduct.getBand(referenceBand).getGeoCoding();
                    this.polygonRegion = this.productSubsetByPolygon.getSubsetPolygonProjectedToGeocoding(referenceBandGeocoding);
                    this.region = this.productSubsetByPolygon.getExtentOfPolygonProjectedToGeocoding(referenceBandGeocoding);
                } else {
                    this.polygonRegion = this.productSubsetByPolygon.getSubsetPolygon();
                    this.region = this.productSubsetByPolygon.getExtentOfPolygon();
                }
                this.geoRegion = null;
                this.subSamplingX = 1;
                this.subSamplingY = 1;
                subsetDef.setSubsetPolygon(this.polygonRegion);
            }
        } catch (Throwable t) {
            throw new OperatorException(t);
        }

        if (geoRegion != null) {
            // the geometry region is specified
            if (isMultisize) {
                // the source product is multisize
                subsetDef.setRegionMap(computeRegionMap(geoRegion, sourceProduct, subsetDef.getNodeNames()));
                region = null;
            } else {
                region = computePixelRegion(sourceProduct, geoRegion, 0);
            }

            if (region != null && region.isEmpty()) {
                targetProduct = new Product("Empty_" + sourceProduct.getName(), "EMPTY", 0, 0);
                String msg = "No intersection with source product boundary " + sourceProduct.getName();
                targetProduct.setDescription(msg);
                getLogger().log(Level.WARNING, msg);
                return;
            }
        }
        if (fullSwath && region != null && sourceProduct != null) {
            region = new Rectangle(0, region.y, sourceProduct.getSceneRasterWidth(), region.height);
        }

        if (region != null && !region.isEmpty()) {
            // there is a non empty pixel region
            if (!isMultisize || referenceBand == null) {
                if (region.width == 0 || region.x + region.width > sourceProduct.getSceneRasterWidth()) {
                    region.width = sourceProduct.getSceneRasterWidth() - region.x;
                }
                if (region.height == 0 || region.y + region.height > sourceProduct.getSceneRasterHeight()) {
                    region.height = sourceProduct.getSceneRasterHeight() - region.y;
                }
                subsetDef.setSubsetRegion(new PixelSubsetRegion(region, 0));
            } else {
                // the source product is multisize and the reference band is specified
                subsetDef.setRegionMap(computeRegionMap(region, polygonRegion, referenceBand, sourceProduct, subsetDef.getNodeNames()));
            }
        }

        if (region == null && geoRegion == null && subsetDef.getNodeNames() != null) {
            HashMap<String, SubsetRegionInfo> regionMap = new HashMap<>();
            for (String nodeName : subsetDef.getNodeNames()) {
                RasterDataNode rdn = sourceProduct.getRasterDataNode(nodeName);
                if (rdn == null || rdn.getRasterWidth() == 0 || rdn.getRasterHeight() == 0) {
                    continue;
                }
                final Rectangle rectangle = new Rectangle(rdn.getRasterWidth(), rdn.getRasterHeight());
                regionMap.put(nodeName, new SubsetRegionInfo(rectangle,null));
            }
            if (regionMap.size() > 0) {
                subsetDef.setRegionMap(regionMap);
            }
        }

        subsetDef.setSubSampling(subSamplingX, subSamplingY);
        subsetDef.setIgnoreMetadata(!copyMetadata);
        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
            targetProduct.setName("Subset_" + targetProduct.getName());
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private void collectReferencedRasters(String nodeName, ArrayList<String> referencedNodeNames) {
        RasterDataNode rasterDataNode = sourceProduct.getRasterDataNode(nodeName);
        if (rasterDataNode == null) {
            throw new OperatorException(String.format("Source product does not contain a raster named '%s'.", nodeName));
        }
        final String validPixelExpression = rasterDataNode.getValidPixelExpression();
        collectReferencedRastersInExpression(validPixelExpression, referencedNodeNames);
        if (rasterDataNode instanceof VirtualBand) {
            VirtualBand vBand = (VirtualBand) rasterDataNode;
            collectReferencedRastersInExpression(vBand.getExpression(), referencedNodeNames);
        }
    }

    private void collectReferencedRastersInExpression(String expression, ArrayList<String> referencedNodeNames) {
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        try {
            final Term term = sourceProduct.parseExpression(expression);
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(term);
            for (RasterDataNode refRaster : refRasters) {
                final String refNodeName = refRaster.getName();
                if (!referencedNodeNames.contains(refNodeName)) {
                    referencedNodeNames.add(refNodeName);
                    collectReferencedRastersInExpression(refNodeName, referencedNodeNames);
                }
            }
        } catch (ParseException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        ProductData destBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(band,
                                            rectangle.x,
                                            rectangle.y,
                                            rectangle.width,
                                            rectangle.height,
                                            destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    // todo - nf/mz 20131105 - move this method to a more prominent location (e.g. FeatureUtils)

    /**
     * Non-API (yet).
     */
    public static Rectangle computePixelRegion(Product product, Geometry geometryRegion, int numBorderPixels) {
        return GeoUtils.computePixelRegionUsingGeometry(product.getSceneGeoCoding(), product.getSceneRasterWidth(), product.getSceneRasterHeight(), geometryRegion, numBorderPixels, false,false);
    }

    public static HashMap<String, SubsetRegionInfo> computeRegionMap(Rectangle region, Product product, String[] rasterNames) {
        if (rasterNames == null || rasterNames.length == 0) {
            List<RasterDataNode> rasterDataNodes = product.getRasterDataNodes();
            rasterNames = new String[rasterDataNodes.size()];
            for (int i = 0; i < rasterDataNodes.size(); i++) {
                rasterNames[i] = rasterDataNodes.get(i).getName();
            }
        }

        HashMap<String, Rectangle> regionMap = new HashMap<>();
        HashMap<String, Geometry> geometryMap = new HashMap<>();
        HashMap<String, SubsetRegionInfo> finalRegionMap = new HashMap<>();

        GeoCoding productGeoCoding = product.getSceneGeoCoding();
        int productWidth = product.getSceneRasterWidth();
        int productHeight = product.getSceneRasterHeight();
        Rectangle pixelRegion = region;
        if (pixelRegion == null) {
            pixelRegion = new Rectangle(productWidth, productHeight);
        }
        Geometry geoRegion = GeoUtils.computeGeometryUsingPixelRegion(productGeoCoding, pixelRegion);
        Geometry finalGeometry = null;
        for (String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
            if (rasterDataNode == null) {
                continue;
            }
            Rectangle rect = GeoUtils.computePixelRegionUsingGeometry(rasterDataNode.getGeoCoding(), rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight(), geoRegion, 0, true,false);
            if (rect.isEmpty()){
                continue;
            }
            regionMap.put(rasterDataNode.getName(), rect);

            GeoCoding rasterGeoCoding = rasterDataNode.getGeoCoding();
            Geometry geom = GeoUtils.computeGeometryUsingPixelRegion(rasterGeoCoding, rect);
            geometryMap.put(rasterDataNode.getName(), geom);
            if (finalGeometry == null) {
                finalGeometry = geom;
            } else if (geom.covers(finalGeometry)) {
                finalGeometry = geom;
            }
        }

        for (String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
            if (rasterDataNode == null) {
                continue;
            }
            Rectangle rect = GeoUtils.computePixelRegionUsingGeometry(rasterDataNode.getGeoCoding(), rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight(), finalGeometry, 0, true,false);
            finalRegionMap.put(rasterDataNode.getName(), new SubsetRegionInfo(rect, null));
        }

        return finalRegionMap;
    }

    public static HashMap<String, SubsetRegionInfo> computeRegionMap(Rectangle region, String referenceBandName, Product product, String[] rasterNames) {
        return computeRegionMap(region, null, referenceBandName, product, rasterNames);
    }

    public static HashMap<String, SubsetRegionInfo> computeRegionMap(Rectangle region, Polygon regionPolygon, String referenceBandName, Product product, String[] rasterNames) {
        if (rasterNames == null || rasterNames.length == 0) {
            List<RasterDataNode> rasterDataNodes = product.getRasterDataNodes();
            rasterNames = new String[rasterDataNodes.size()];
            for (int i = 0; i < rasterDataNodes.size(); i++) {
                rasterNames[i] = rasterDataNodes.get(i).getName();
            }
        }

        HashMap<String, SubsetRegionInfo> regionMap = new HashMap<>();
        RasterDataNode referenceNode = product.getBand(referenceBandName);
        if (referenceNode == null) {
            referenceNode = product.getBandAt(0);
        }

        GeoCoding referenceRasterGeoCoding = referenceNode.getGeoCoding();
        int referenceRasterWidth = referenceNode.getRasterWidth();
        int referenceRasterHeight = referenceNode.getRasterHeight();
        Rectangle pixelRegion = region;
        if (pixelRegion == null) {
            pixelRegion = new Rectangle(referenceRasterWidth, referenceRasterHeight);
        }

        Geometry geometryRegion = GeoUtils.computeGeometryUsingPixelRegion(referenceRasterGeoCoding, pixelRegion);

        for (String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
            if (rasterDataNode == null) {
                continue;
            }
            if (rasterDataNode.getGeoCoding().equals(referenceNode.getGeoCoding())) {
                regionMap.put(rasterDataNode.getName(), new SubsetRegionInfo(region, regionPolygon));
                continue;
            }
            boolean usePixelCenter = true;
            boolean multiSize = false;
            if(product.isMultiSize()){
                multiSize = true;
                usePixelCenter = false;
            }
            Rectangle rasterPixelRegion = GeoUtils.computePixelRegionUsingGeometry(rasterDataNode.getGeoCoding(), rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight(), geometryRegion, 0, usePixelCenter, multiSize);
            final Polygon rasterRegionPolygon = GeoUtils.projectPolygonToGeocoding(regionPolygon, referenceRasterGeoCoding, rasterDataNode.getGeoCoding());
            regionMap.put(rasterDataNode.getName(), new SubsetRegionInfo(rasterPixelRegion, rasterRegionPolygon));
        }

        return regionMap;
    }

    public static HashMap<String, SubsetRegionInfo> computeRegionMap(Geometry geoRegion, Product product, String[] rasterNames) {
        if (rasterNames == null || rasterNames.length == 0) {
            List<RasterDataNode> rasterDataNodes = product.getRasterDataNodes();
            rasterNames = new String[rasterDataNodes.size()];
            for (int i = 0; i < rasterDataNodes.size(); i++) {
                rasterNames[i] = rasterDataNodes.get(i).getName();
            }
        }

        HashMap<String, SubsetRegionInfo> regionMap = new HashMap<>();

        for (String rasterName : rasterNames) {
            RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
            if (rasterDataNode == null) {
                continue;
            }

            Rectangle rect = GeoUtils.computePixelRegionUsingGeometry(rasterDataNode.getGeoCoding(), rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight(), geoRegion, 0, true, false);
            regionMap.put(rasterDataNode.getName(), new SubsetRegionInfo(rect, null));
        }

        return regionMap;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SubsetOp.class);
        }
    }
}
