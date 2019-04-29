package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.PointingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

public class OperatorProduct extends Product {

    private final Operator operator;
    private boolean executionTriggered;
    private boolean initialized;

    /**
     * The list of product listeners.
     */
    private List<ProductNodeListener> listeners;

    /**
     * The internal reference string of this product
     */
    private String refStr;

    public OperatorProduct(Operator operator, Product actualProduct) {
        // do this for adhering to the interface. We will always delegate calls to the actual product.
        super(actualProduct.getName(), actualProduct.getProductType(),
                actualProduct.getSceneRasterWidth(), actualProduct.getSceneRasterHeight());
        ProductUtils.copyProductNodes(actualProduct, this);
        setSceneTimeCoding(actualProduct.getSceneTimeCoding());
        setPreferredTileSize(actualProduct.getPreferredTileSize());
        for (Band band : actualProduct.getBands()) {
            Band copiedBand;
            if (band instanceof VirtualBand) {
                copiedBand = ProductUtils.copyVirtualBand(this, (VirtualBand) band, band.getName());
            } else {
                copiedBand = ProductUtils.copyBand(band.getName(), actualProduct, this, false);
                if (band.isSourceImageSet()) {
                    copiedBand.setSourceImage(band.getSourceImage());
                }
            }
            if (band.getGeoCoding() != actualProduct.getSceneGeoCoding()) {
                copiedBand.setGeoCoding(band.getGeoCoding());
            }
        }
        if (actualProduct.getProductReader() != null) {
            setProductReader(actualProduct.getProductReader());
        }
        setFileLocation(actualProduct.getFileLocation());
        this.operator = operator;
        executionTriggered = false;
        initialized = false;
    }

//    private Product triggerExecution();super {
//        if (!executionTriggered) {
//            executionTriggered = true;
//            operator.execute(ProgressMonitor.NULL);
//        }
//        return actualProduct;
//    }

    private synchronized void triggerExecution() {
        if (!executionTriggered && initialized) {
            executionTriggered = true;
            operator.execute(ProgressMonitor.NULL);
        }
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

//    public CoordinateReferenceSystem getSceneCRS() {
//        return triggerExecution();super.getSceneCRS();
//    }

//    public void setSceneCRS(CoordinateReferenceSystem sceneCRS) {
//        triggerExecution();super.setSceneCRS(sceneCRS);
//    }

//    public boolean isSceneCrsASharedModelCrs() {
//        return triggerExecution();super.isSceneCrsASharedModelCrs();
//    }

//    public boolean isSceneCrsEqualToModelCrsOf(RasterDataNode rasterDataNode) {
//        return triggerExecution();super.isSceneCrsEqualToModelCrsOf(rasterDataNode);
//    }

//    public File getFileLocation() {
//        return triggerExecution();super.getFileLocation();
//    }

//    public void setFileLocation(final File fileLocation) {
//        triggerExecution();super.setFileLocation(fileLocation);
//    }

    //////////////////////////////////////////////////////////////////////////
    // Attribute Query

//    public String getProductType() {
//        return triggerExecution();super.getProductType();
//    }

//    public void setProductType(final String productType) {
//        triggerExecution();super.setProductType(productType);
//    }

//    public void setProductReader(final ProductReader reader) {
//        triggerExecution();super.setProductReader(reader);
//    }

//    @Override
//    public ProductReader getProductReader() {
//        return triggerExecution();super.getProductReader();
//    }

    public void setProductWriter(final ProductWriter writer) {
        triggerExecution();
        super.setProductWriter(writer);
    }

    @Override
    public ProductWriter getProductWriter() {
        triggerExecution();
        return super.getProductWriter();
    }

    public void writeHeader(Object output) throws IOException {
        triggerExecution();
        super.writeHeader(output);
    }

    public void closeProductReader() throws IOException {
        triggerExecution();
        super.closeProductReader();
    }

    public void closeProductWriter() throws IOException {
        triggerExecution();
        super.closeProductWriter();
    }

    public void closeIO() throws IOException {
        triggerExecution();
        super.closeIO();
    }

    public TimeCoding getSceneTimeCoding() {
        triggerExecution();
        return super.getSceneTimeCoding();
    }

    public void setSceneTimeCoding(final TimeCoding sceneTimeCoding) {
        triggerExecution();
        super.setSceneTimeCoding(sceneTimeCoding);
    }

    public PointingFactory getPointingFactory() {
        triggerExecution();
        return super.getPointingFactory();
    }

    public void setPointingFactory(PointingFactory pointingFactory) {
        triggerExecution();
        super.setPointingFactory(pointingFactory);
    }

    public void setSceneGeoCoding(final GeoCoding sceneGeoCoding) {
        triggerExecution();
        super.setSceneGeoCoding(sceneGeoCoding);
    }

    public GeoCoding getSceneGeoCoding() {
        triggerExecution();
        return super.getSceneGeoCoding();
    }

    public boolean isUsingSingleGeoCoding() {
        triggerExecution();
        return super.isUsingSingleGeoCoding();
    }

    public boolean transferGeoCodingTo(final Product destProduct, final ProductSubsetDef subsetDef) {
        triggerExecution();
        return super.transferGeoCodingTo(destProduct, subsetDef);
    }

    public boolean isMultiSize() {
        triggerExecution();
        return super.isMultiSize();
    }

    public final Dimension getSceneRasterSize() {
        triggerExecution();
        return super.getSceneRasterSize();
    }

    public ProductData.UTC getStartTime() {
        triggerExecution();
        return super.getStartTime();
    }

    public void setStartTime(final ProductData.UTC startTime) {
        triggerExecution();super.setStartTime(startTime);
    }

    public ProductData.UTC getEndTime() {
        triggerExecution();
        return super.getEndTime();
    }

    public void setEndTime(final ProductData.UTC endTime) {
        triggerExecution();
        super.setEndTime(endTime);
    }

    public MetadataElement getMetadataRoot() {
        triggerExecution();
        return super.getMetadataRoot();
    }

    //////////////////////////////////////////////////////////////////////////
    // Group support

    public ProductNodeGroup<ProductNodeGroup> getGroups() {
        triggerExecution();
        return super.getGroups();
    }

    public ProductNodeGroup getGroup(String name) {
        triggerExecution();
        return super.getGroup(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Tie-point grid support

    public ProductNodeGroup<TiePointGrid> getTiePointGridGroup() {
        triggerExecution();
        return super.getTiePointGridGroup();
    }

    public void addTiePointGrid(final TiePointGrid tiePointGrid) {
        triggerExecution();
        super.addTiePointGrid(tiePointGrid);
    }

    public boolean removeTiePointGrid(final TiePointGrid tiePointGrid) {
        triggerExecution();
        return super.removeTiePointGrid(tiePointGrid);
    }

    public int getNumTiePointGrids() {
        triggerExecution();
        return super.getNumTiePointGrids();
    }

    public TiePointGrid getTiePointGridAt(final int index) {
        triggerExecution();
        return super.getTiePointGridAt(index);
    }

    public String[] getTiePointGridNames() {
        triggerExecution();
        return super.getTiePointGridNames();
    }

    public TiePointGrid[] getTiePointGrids() {
        triggerExecution();
        return super.getTiePointGrids();
    }

    public TiePointGrid getTiePointGrid(final String name) {
        triggerExecution();
        return super.getTiePointGrid(name);
    }

    public boolean containsTiePointGrid(final String name) {
        triggerExecution();
        return super.containsTiePointGrid(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Band support

    public ProductNodeGroup<Band> getBandGroup() {
        triggerExecution();
        return super.getBandGroup();
    }

//    public void addBand(final Band band) {
//        triggerExecution();
//        super.addBand(band);
//    }


//    public boolean removeBand(final Band band) {
//        return triggerExecution();super.removeBand(band);
//    }

    public int getNumBands() {
        triggerExecution();
        return super.getNumBands();
    }

    public Band getBandAt(final int index) {
        triggerExecution();
        return super.getBandAt(index);
    }

    public String[] getBandNames() {
        triggerExecution();
        return super.getBandNames();
    }

    public Band[] getBands() {
        triggerExecution();
        return super.getBands();
    }

    public Band getBand(final String name) {
        triggerExecution();
        return super.getBand(name);
    }

    public int getBandIndex(final String name) {
        triggerExecution();
        return super.getBandIndex(name);
    }

    public boolean containsBand(final String name) {
        triggerExecution();
        return super.containsBand(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Quicklook support

    public ProductNodeGroup<Quicklook> getQuicklookGroup() {
        triggerExecution();
        return super.getQuicklookGroup();
    }

    public Quicklook getDefaultQuicklook() {
        triggerExecution();
        return super.getDefaultQuicklook();
    }

    public Quicklook getQuicklook(final String name) {
        triggerExecution();
        return super.getQuicklook(name);
    }

    public String getQuicklookBandName() {
        triggerExecution();
        return super.getQuicklookBandName();
    }

    public void setQuicklookBandName(String quicklookBandName) {
        triggerExecution();
        super.setQuicklookBandName(quicklookBandName);
    }

    //////////////////////////////////////////////////////////////////////////
    // Mask support

    public ProductNodeGroup<Mask> getMaskGroup() {
        triggerExecution();
        return super.getMaskGroup();
    }

    //////////////////////////////////////////////////////////////////////////
    // Vector data support

    public ProductNodeGroup<VectorDataNode> getVectorDataGroup() {
        triggerExecution();
        return super.getVectorDataGroup();
    }

    //////////////////////////////////////////////////////////////////////////
    // Sample-coding support

    public ProductNodeGroup<FlagCoding> getFlagCodingGroup() {
        triggerExecution();
        return super.getFlagCodingGroup();
    }

    public ProductNodeGroup<IndexCoding> getIndexCodingGroup() {
        triggerExecution();
        return super.getIndexCodingGroup();
    }

    public PlacemarkGroup getGcpGroup() {
        triggerExecution();
        return super.getGcpGroup();
    }

    public synchronized PlacemarkGroup getPinGroup() {
        triggerExecution();
        return super.getPinGroup();
    }

    //
    //////////////////////////////////////////////////////////////////////////

    public int getNumResolutionsMax() {
        triggerExecution();
        return super.getNumResolutionsMax();
    }

    public void setNumResolutionsMax(int numResolutionsMax) {
        triggerExecution();
        super.setNumResolutionsMax(numResolutionsMax);
    }

    public Term parseExpression(final String expression) throws ParseException {
        triggerExecution();
        return super.parseExpression(expression);
    }

    public RasterDataNode[] getRefRasterDataNodes(String expression) throws ParseException {
        triggerExecution();
        return super.getRefRasterDataNodes(expression);
    }

    //////////////////////////////////////////////////////////////////////////
    // Utilities

    /**
     * @return All removed child nodes. Array may be empty.
     */
    public ProductNode[] getRemovedChildNodes() {
        triggerExecution();
        return super.getRemovedChildNodes();
    }

    public Dimension getPreferredTileSize() {
        triggerExecution();
        return super.getPreferredTileSize();
    }

    public void setPreferredTileSize(int tileWidth, int tileHeight) {
        triggerExecution();
        super.setPreferredTileSize(tileWidth, tileHeight);
    }

    public void setPreferredTileSize(Dimension preferredTileSize) {
        triggerExecution();
        super.setPreferredTileSize(preferredTileSize);
    }

    public Product.AutoGrouping getAutoGrouping() {
        triggerExecution();
        return super.getAutoGrouping();
    }

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API

    public MultiLevelImage getMaskImage(String expression, RasterDataNode associatedRaster) {
        triggerExecution();
        return super.getMaskImage(expression, associatedRaster);
    }

}
