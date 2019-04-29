package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.DataNode;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.PointingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductVisitor;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

class ProxyProduct extends Product {

    private final Operator operator;

    /**
     * The internal reference string of this product
     */
    private String refStr;

    ProxyProduct(Operator operator) {
        // do this for adhering to the interface. We will always delegate calls to the actual product.
        super("", "");
        this.operator = operator;
    }

    private Product getActualProduct() {
        Product actualProduct = operator.getTargetProduct();
        operator.execute(ProgressMonitor.NULL);
        return actualProduct;
    }

    public CoordinateReferenceSystem getSceneCRS() {
        return getActualProduct().getSceneCRS();
    }

    public void setSceneCRS(CoordinateReferenceSystem sceneCRS) {
        getActualProduct().setSceneCRS(sceneCRS);
    }

    public boolean isSceneCrsASharedModelCrs() {
        return getActualProduct().isSceneCrsASharedModelCrs();
    }

    public boolean isSceneCrsEqualToModelCrsOf(RasterDataNode rasterDataNode) {
        return getActualProduct().isSceneCrsEqualToModelCrsOf(rasterDataNode);
    }

    public File getFileLocation() {
        return getActualProduct().getFileLocation();
    }

    public void setFileLocation(final File fileLocation) {
        getActualProduct().setFileLocation(fileLocation);
    }

    //////////////////////////////////////////////////////////////////////////
    // Attribute Query

    public String getProductType() {
        return getActualProduct().getProductType();
    }

    public void setProductType(final String productType) {
        getActualProduct().setProductType(productType);
    }

    public void setProductReader(final ProductReader reader) {
        getActualProduct().setProductReader(reader);
    }

    @Override
    public ProductReader getProductReader() {
        return getActualProduct().getProductReader();
    }

    public void setProductWriter(final ProductWriter writer) {
        getActualProduct().setProductWriter(writer);
    }

    @Override
    public ProductWriter getProductWriter() {
        return getActualProduct().getProductWriter();
    }

    public void writeHeader(Object output) throws IOException {
        getActualProduct().writeHeader(output);
    }

    public void closeProductReader() throws IOException {
        getActualProduct().closeProductReader();
    }

    public void closeProductWriter() throws IOException {
        getActualProduct().closeProductWriter();
    }

    public void closeIO() throws IOException {
        getActualProduct().closeIO();
    }

    @Override
    public void dispose() {
        getActualProduct().dispose();
    }

    public TimeCoding getSceneTimeCoding() {
        return getActualProduct().getSceneTimeCoding();
    }

    public void setSceneTimeCoding(final TimeCoding sceneTimeCoding) {
        getActualProduct().setSceneTimeCoding(sceneTimeCoding);
    }

    public PointingFactory getPointingFactory() {
        return getActualProduct().getPointingFactory();
    }

    public void setPointingFactory(PointingFactory pointingFactory) {
        getActualProduct().setPointingFactory(pointingFactory);
    }

    public void setSceneGeoCoding(final GeoCoding sceneGeoCoding) {
        getActualProduct().setSceneGeoCoding(sceneGeoCoding);
    }

    public GeoCoding getSceneGeoCoding() {
        return getActualProduct().getSceneGeoCoding();
    }

    public boolean isUsingSingleGeoCoding() {
        return getActualProduct().isUsingSingleGeoCoding();
    }

    public boolean transferGeoCodingTo(final Product destProduct, final ProductSubsetDef subsetDef) {
        return getActualProduct().transferGeoCodingTo(destProduct, subsetDef);
    }

    public boolean isMultiSize() {
        return getActualProduct().isMultiSize();
    }

    public final Dimension getSceneRasterSize() {
        return getActualProduct().getSceneRasterSize();
    }

    public ProductData.UTC getStartTime() {
        return getActualProduct().getStartTime();
    }

    public void setStartTime(final ProductData.UTC startTime) {
        getActualProduct().setStartTime(startTime);
    }

    public ProductData.UTC getEndTime() {
        return getActualProduct().getEndTime();
    }

    public void setEndTime(final ProductData.UTC endTime) {
        getActualProduct().setEndTime(endTime);
    }

    public MetadataElement getMetadataRoot() {
        return getActualProduct().getMetadataRoot();
    }

    //////////////////////////////////////////////////////////////////////////
    // Group support

    public ProductNodeGroup<ProductNodeGroup> getGroups() {
        return getActualProduct().getGroups();
    }

    public ProductNodeGroup getGroup(String name) {
        return getActualProduct().getGroup(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Tie-point grid support

    public ProductNodeGroup<TiePointGrid> getTiePointGridGroup() {
        return getActualProduct().getTiePointGridGroup();
    }

    public void addTiePointGrid(final TiePointGrid tiePointGrid) {
        getActualProduct().addTiePointGrid(tiePointGrid);
    }

    public boolean removeTiePointGrid(final TiePointGrid tiePointGrid) {
        return getActualProduct().removeTiePointGrid(tiePointGrid);
    }

    public int getNumTiePointGrids() {
        return getActualProduct().getNumTiePointGrids();
    }

    public TiePointGrid getTiePointGridAt(final int index) {
        return getActualProduct().getTiePointGridAt(index);
    }

    public String[] getTiePointGridNames() {
        return getActualProduct().getTiePointGridNames();
    }

    public TiePointGrid[] getTiePointGrids() {
        return getActualProduct().getTiePointGrids();
    }

    public TiePointGrid getTiePointGrid(final String name) {
        return getActualProduct().getTiePointGrid(name);
    }

    public boolean containsTiePointGrid(final String name) {
        return getActualProduct().containsTiePointGrid(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Band support

    public ProductNodeGroup<Band> getBandGroup() {
        return getActualProduct().getBandGroup();
    }

    public void addBand(final Band band) {
        getActualProduct().addBand(band);
    }


    public boolean removeBand(final Band band) {
        return getActualProduct().removeBand(band);
    }

    public int getNumBands() {
        return getActualProduct().getNumBands();
    }

    public Band getBandAt(final int index) {
        return getActualProduct().getBandAt(index);
    }

    public String[] getBandNames() {
        return getActualProduct().getBandNames();
    }

    public Band[] getBands() {
        return getActualProduct().getBands();
    }

    public Band getBand(final String name) {
        return getActualProduct().getBand(name);
    }

    public int getBandIndex(final String name) {
        return getActualProduct().getBandIndex(name);
    }

    public boolean containsBand(final String name) {
        return getActualProduct().containsBand(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Quicklook support

    public ProductNodeGroup<Quicklook> getQuicklookGroup() {
        return getActualProduct().getQuicklookGroup();
    }

    public Quicklook getDefaultQuicklook() {
        return getActualProduct().getDefaultQuicklook();
    }

    public Quicklook getQuicklook(final String name) {
        return getActualProduct().getQuicklook(name);
    }

    public String getQuicklookBandName() {
        return getActualProduct().getQuicklookBandName();
    }

    public void setQuicklookBandName(String quicklookBandName) {
        getActualProduct().setQuicklookBandName(quicklookBandName);
    }

    //////////////////////////////////////////////////////////////////////////
    // Mask support

    public ProductNodeGroup<Mask> getMaskGroup() {
        return getActualProduct().getMaskGroup();
    }

    //////////////////////////////////////////////////////////////////////////
    // Vector data support

    public ProductNodeGroup<VectorDataNode> getVectorDataGroup() {
        return getActualProduct().getVectorDataGroup();
    }

    //////////////////////////////////////////////////////////////////////////
    // Sample-coding support

    public ProductNodeGroup<FlagCoding> getFlagCodingGroup() {
        return getActualProduct().getFlagCodingGroup();
    }

    public ProductNodeGroup<IndexCoding> getIndexCodingGroup() {
        return getActualProduct().getIndexCodingGroup();
    }

    public PlacemarkGroup getGcpGroup() {
        return getActualProduct().getGcpGroup();
    }

    public synchronized PlacemarkGroup getPinGroup() {
        return getActualProduct().getPinGroup();
    }

    //
    //////////////////////////////////////////////////////////////////////////

    public int getNumResolutionsMax() {
        return getActualProduct().getNumResolutionsMax();
    }

    public void setNumResolutionsMax(int numResolutionsMax) {
        getActualProduct().setNumResolutionsMax(numResolutionsMax);
    }

    public Term parseExpression(final String expression) throws ParseException {
        return getActualProduct().parseExpression(expression);
    }

    public RasterDataNode[] getRefRasterDataNodes(String expression) throws ParseException {
        return getActualProduct().getRefRasterDataNodes(expression);
    }


    //////////////////////////////////////////////////////////////////////////
    // Visitor-Pattern support

    @Override
    public void acceptVisitor(final ProductVisitor visitor) {
        getActualProduct().acceptVisitor(visitor);
    }

    //////////////////////////////////////////////////////////////////////////
    // Product listener support

    public boolean addProductNodeListener(final ProductNodeListener listener) {
        return getActualProduct().addProductNodeListener(listener);
    }

    public void removeProductNodeListener(final ProductNodeListener listener) {
        getActualProduct().removeProductNodeListener(listener);
    }

    public ProductNodeListener[] getProductNodeListeners() {
        return getActualProduct().getProductNodeListeners();
    }

    protected boolean hasProductNodeListeners() {
        return getActualProduct().getProductNodeListeners().length > 0;
    }

    protected void fireNodeChanged(ProductNode sourceNode, String propertyName, Object oldValue, Object newValue) {
        fireEvent(sourceNode, propertyName, oldValue, newValue);
    }

    protected void fireNodeDataChanged(DataNode sourceNode) {
        fireEvent(sourceNode, ProductNodeEvent.NODE_DATA_CHANGED, null);
    }

    protected void fireNodeAdded(ProductNode childNode, ProductNodeGroup nodeGroup) {
        fireEvent(childNode, ProductNodeEvent.NODE_ADDED, nodeGroup);
    }

    protected void fireNodeRemoved(ProductNode childNode, ProductNodeGroup nodeGroup) {
        fireEvent(childNode, ProductNodeEvent.NODE_REMOVED, nodeGroup);
    }

    private void fireEvent(final ProductNode sourceNode, int eventType, ProductNodeGroup nodeGroup) {
        if (hasProductNodeListeners()) {
            final ProductNodeEvent event = new ProductNodeEvent(sourceNode, eventType, nodeGroup);
            fireEvent(event);
        }
    }

    private void fireEvent(final ProductNode sourceNode, final String propertyName, Object oldValue, Object newValue) {
        if (hasProductNodeListeners()) {
            final ProductNodeEvent event = new ProductNodeEvent(sourceNode, propertyName, oldValue, newValue);
            fireEvent(event);
        }
    }

    private void fireEvent(final ProductNodeEvent event) {
        fireEvent(event, getProductNodeListeners());
    }

    static void fireEvent(final ProductNodeEvent event, final ProductNodeListener[] productNodeListeners) {
        for (ProductNodeListener listener : productNodeListeners) {
            fireEvent(event, listener);
        }
    }

    private static void fireEvent(final ProductNodeEvent event, final ProductNodeListener listener) {
        switch (event.getType()) {
            case ProductNodeEvent.NODE_CHANGED:
                listener.nodeChanged(event);
                break;
            case ProductNodeEvent.NODE_DATA_CHANGED:
                listener.nodeDataChanged(event);
                break;
            case ProductNodeEvent.NODE_ADDED:
                listener.nodeAdded(event);
                break;
            case ProductNodeEvent.NODE_REMOVED:
                listener.nodeRemoved(event);
                break;
        }
    }

    public int getRefNo() {
        return getActualProduct().getRefNo();
    }

    public void setRefNo(final int refNo) {
        getActualProduct().setRefNo(refNo);
        refStr = "[" + refNo + "]";
    }

    public void resetRefNo() {
        getActualProduct().resetRefNo();
        refStr = null;
    }

    String getRefStr() {
        return refStr;
    }

    public ProductManager getProductManager() {
        return getActualProduct().getProductManager();
    }

    public void setProductManager(final ProductManager productManager) {
        getActualProduct().setProductManager(productManager);
    }

    //////////////////////////////////////////////////////////////////////////
    // Utilities

    @Override
    public void setModified(final boolean modified) {
        getActualProduct().setModified(modified);
    }

    /**
     * @return All removed child nodes. Array may be empty.
     */
    public ProductNode[] getRemovedChildNodes() {
        return getActualProduct().getRemovedChildNodes();
    }

    public Dimension getPreferredTileSize() {
        return getActualProduct().getPreferredTileSize();
    }

    public void setPreferredTileSize(int tileWidth, int tileHeight) {
        getActualProduct().setPreferredTileSize(tileWidth, tileHeight);
    }

    public void setPreferredTileSize(Dimension preferredTileSize) {
        getActualProduct().setPreferredTileSize(preferredTileSize);
    }

    public Product.AutoGrouping getAutoGrouping() {
        return getActualProduct().getAutoGrouping();
    }

    public void setAutoGrouping(Product.AutoGrouping autoGrouping) {
        getActualProduct().setAutoGrouping(autoGrouping);
    }

    public void setAutoGrouping(String pattern) {
        getActualProduct().setAutoGrouping(pattern);
    }

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API

    public MultiLevelImage getMaskImage(String expression, RasterDataNode associatedRaster) {
        return getActualProduct().getMaskImage(expression, associatedRaster);
    }

}
