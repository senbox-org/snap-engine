//package org.esa.snap.core.gpf;
//
//import com.bc.ceres.core.Assert;
//import com.bc.ceres.core.ProgressMonitor;
//import com.bc.ceres.glevel.MultiLevelImage;
//import org.esa.snap.core.dataio.ProductReader;
//import org.esa.snap.core.dataio.ProductSubsetBuilder;
//import org.esa.snap.core.dataio.ProductSubsetDef;
//import org.esa.snap.core.dataio.ProductWriter;
//import org.esa.snap.core.datamodel.Band;
//import org.esa.snap.core.datamodel.DataNode;
//import org.esa.snap.core.datamodel.FlagCoding;
//import org.esa.snap.core.datamodel.GeoCoding;
//import org.esa.snap.core.datamodel.GeoPos;
//import org.esa.snap.core.datamodel.IndexCoding;
//import org.esa.snap.core.datamodel.MapGeoCoding;
//import org.esa.snap.core.datamodel.Mask;
//import org.esa.snap.core.datamodel.MetadataAttribute;
//import org.esa.snap.core.datamodel.MetadataElement;
//import org.esa.snap.core.datamodel.PixelPos;
//import org.esa.snap.core.datamodel.Placemark;
//import org.esa.snap.core.datamodel.PlacemarkGroup;
//import org.esa.snap.core.datamodel.PointingFactory;
//import org.esa.snap.core.datamodel.Product;
//import org.esa.snap.core.datamodel.ProductData;
//import org.esa.snap.core.datamodel.ProductManager;
//import org.esa.snap.core.datamodel.ProductNode;
//import org.esa.snap.core.datamodel.ProductNodeEvent;
//import org.esa.snap.core.datamodel.ProductNodeGroup;
//import org.esa.snap.core.datamodel.ProductNodeListener;
//import org.esa.snap.core.datamodel.ProductVisitor;
//import org.esa.snap.core.datamodel.RasterDataNode;
//import org.esa.snap.core.datamodel.TiePointGeoCoding;
//import org.esa.snap.core.datamodel.TiePointGrid;
//import org.esa.snap.core.datamodel.TimeCoding;
//import org.esa.snap.core.datamodel.VectorDataNode;
//import org.esa.snap.core.datamodel.VirtualBand;
//import org.esa.snap.core.datamodel.VirtualBandMultiLevelImage;
//import org.esa.snap.core.datamodel.quicklooks.Quicklook;
//import org.esa.snap.core.dataop.barithm.BandArithmetic;
//import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
//import org.esa.snap.core.dataop.barithm.SingleFlagSymbol;
//import org.esa.snap.core.dataop.maptransf.MapInfo;
//import org.esa.snap.core.dataop.maptransf.MapProjection;
//import org.esa.snap.core.dataop.maptransf.MapTransform;
//import org.esa.snap.core.jexp.Namespace;
//import org.esa.snap.core.jexp.ParseException;
//import org.esa.snap.core.jexp.Parser;
//import org.esa.snap.core.jexp.Term;
//import org.esa.snap.core.jexp.WritableNamespace;
//import org.esa.snap.core.jexp.impl.ParserImpl;
//import org.esa.snap.core.util.Guardian;
//import org.esa.snap.core.util.ObjectUtils;
//import org.esa.snap.core.util.ProductUtils;
//import org.esa.snap.core.util.StringUtils;
//import org.esa.snap.core.util.SystemUtils;
//import org.esa.snap.core.util.io.WildcardMatcher;
//import org.esa.snap.core.util.math.MathUtils;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.geom.Point2D;
//import java.io.File;
//import java.io.IOException;
//import java.lang.ref.WeakReference;
//import java.util.AbstractList;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.logging.Logger;
//
//public class ProxyProduct extends Product {
//
//    Product actualProduct;
//
//    public ProxyProduct(String name, String type) {
//        super(name, type);
//    }
//
//    private Product getActualProduct() {
//        return actualProduct;
//    }
//
//    public CoordinateReferenceSystem getSceneCRS() {
//        return getActualProduct().getSceneCRS();
//    }
//
//    public void setSceneCRS(CoordinateReferenceSystem sceneCRS) {
//        getActualProduct().setSceneCRS(sceneCRS);
//    }
//
//    public boolean isSceneCrsASharedModelCrs() {
//        return getActualProduct().isSceneCrsASharedModelCrs();
//    }
//
//    public boolean isSceneCrsEqualToModelCrsOf(RasterDataNode rasterDataNode) {
//        return getActualProduct().isSceneCrsEqualToModelCrsOf(rasterDataNode);
//    }
//
//    public File getFileLocation() {
//        return getActualProduct().getFileLocation();
//    }
//
//    public void setFileLocation(final File fileLocation) {
//        getActualProduct().setFileLocation(fileLocation);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Attribute Query
//
//    public String getProductType() {
//        return getActualProduct().getProductType();
//    }
//
//    public void setProductType(final String productType) {
//        getActualProduct().setProductType(productType);
//    }
//
//    public void setProductReader(final ProductReader reader) {
//        getActualProduct().setProductReader(reader);
//    }
//
//    @Override
//    public ProductReader getProductReader() {
//        return getActualProduct().getProductReader();
//    }
//
//    public void setProductWriter(final ProductWriter writer) {
//        getActualProduct().setProductWriter(writer);
//    }
//
//    @Override
//    public ProductWriter getProductWriter() {
//        return getActualProduct().getProductWriter();
//    }
//
//    public void writeHeader(Object output) throws IOException {
//        getActualProduct().writeHeader(output);
//    }
//
//    public void closeProductReader() throws IOException {
//        getActualProduct().closeProductReader();
//    }
//
//    public void closeProductWriter() throws IOException {
//        getActualProduct().closeProductWriter();
//    }
//
//    public void closeIO() throws IOException {
//        getActualProduct().closeIO();
//    }
//
//    @Override
//    public void dispose() {
//        getActualProduct().dispose();
//        actualProduct = null;
//    }
//
//    public TimeCoding getSceneTimeCoding() {
//        return getActualProduct().getSceneTimeCoding();
//    }
//
//    public void setSceneTimeCoding(final TimeCoding sceneTimeCoding) {
//        getActualProduct().setSceneTimeCoding(sceneTimeCoding);
//    }
//
//    public PointingFactory getPointingFactory() {
//        return getActualProduct().getPointingFactory();
//    }
//
//    public void setPointingFactory(PointingFactory pointingFactory) {
//        getActualProduct().setPointingFactory(pointingFactory);
//    }
//
//    public void setSceneGeoCoding(final GeoCoding sceneGeoCoding) {
//        getActualProduct().setSceneGeoCoding(sceneGeoCoding);
//    }
//
//    public GeoCoding getSceneGeoCoding() {
//        return getActualProduct().getSceneGeoCoding();
//    }
//
//    public boolean isUsingSingleGeoCoding() {
//        return getActualProduct().isUsingSingleGeoCoding();
//    }
//
//    public boolean transferGeoCodingTo(final Product destProduct, final ProductSubsetDef subsetDef) {
//        return getActualProduct().transferGeoCodingTo(destProduct, subsetDef);
//    }
//
//    public boolean isMultiSize() {
//        return getActualProduct().isMultiSize();
//    }
//
//    public final Dimension getSceneRasterSize() {
//        if (sceneRasterSize != null) {
//            return sceneRasterSize;
//        }
//        if (!initSceneProperties()) {
//            throw new IllegalStateException("scene raster size not set and no reference band found to derive it from");
//        }
//        return sceneRasterSize;
//    }
//
//    public ProductData.UTC getStartTime() {
//        return getActualProduct().getStartTime();
//    }
//
//    public void setStartTime(final ProductData.UTC startTime) {
//        getActualProduct().setStartTime(startTime);
//    }
//
//    public ProductData.UTC getEndTime() {
//        return getActualProduct().getEndTime();
//    }
//
//    public void setEndTime(final ProductData.UTC endTime) {
//        getActualProduct().setEndTime(endTime);
//    }
//
//    public MetadataElement getMetadataRoot() {
//        return getActualProduct().getMetadataRoot();
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Group support
//
//    public ProductNodeGroup<ProductNodeGroup> getGroups() {
//        return getActualProduct().getGroups();
//    }
//
//    public ProductNodeGroup getGroup(String name) {
//        return getActualProduct().getGroup(name);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Tie-point grid support
//
//    public ProductNodeGroup<TiePointGrid> getTiePointGridGroup() {
//        return getActualProduct().getTiePointGridGroup();
//    }
//
//    public void addTiePointGrid(final TiePointGrid tiePointGrid) {
//        getActualProduct().addTiePointGrid(tiePointGrid);
//    }
//
//    public boolean removeTiePointGrid(final TiePointGrid tiePointGrid) {
//        return getActualProduct().removeTiePointGrid(tiePointGrid);
//    }
//
//    public int getNumTiePointGrids() {
//        return getActualProduct().getNumTiePointGrids();
//    }
//
//    public TiePointGrid getTiePointGridAt(final int index) {
//        return getActualProduct().getTiePointGridAt(index);
//    }
//
//    public String[] getTiePointGridNames() {
//        return getActualProduct().getTiePointGridNames();
//    }
//
//    public TiePointGrid[] getTiePointGrids() {
//        return getActualProduct().getTiePointGrids();
//    }
//
//    public TiePointGrid getTiePointGrid(final String name) {
//        return getActualProduct().getTiePointGrid(name);
//    }
//
//    public boolean containsTiePointGrid(final String name) {
//        return getActualProduct().containsTiePointGrid(name);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Band support
//
//    public ProductNodeGroup<Band> getBandGroup() {
//        return getActualProduct().getBandGroup();
//    }
//
//    public void addBand(final Band band) {
//        getActualProduct().addBand(band);
//    }
//
//
//    public boolean removeBand(final Band band) {
//        return getActualProduct().removeBand(band);
//    }
//
//    public int getNumBands() {
//        return getActualProduct().getNumBands();
//    }
//
//    public Band getBandAt(final int index) {
//        return getActualProduct().getBandAt(index);
//    }
//
//    public String[] getBandNames() {
//        return getActualProduct().getBandNames();
//    }
//
//    public Band[] getBands() {
//        return getActualProduct().getBands();
//    }
//
//    public Band getBand(final String name) {
//        return getActualProduct().getBand(name);
//    }
//
//    public int getBandIndex(final String name) {
//        return getActualProduct().getBandIndex(name);
//    }
//
//    public boolean containsBand(final String name) {
//        return getActualProduct().containsBand(name);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Quicklook support
//
//    public ProductNodeGroup<Quicklook> getQuicklookGroup() {
//        return getActualProduct().getQuicklookGroup();
//    }
//
//    public Quicklook getDefaultQuicklook() {
//        return getActualProduct().getDefaultQuicklook();
//    }
//
//    public Quicklook getQuicklook(final String name) {
//        return getActualProduct().getQuicklook(name);
//    }
//
//    public String getQuicklookBandName() {
//        return getActualProduct().getQuicklookBandName();
//    }
//
//    public void setQuicklookBandName(String quicklookBandName) {
//        getActualProduct().setQuicklookBandName(quicklookBandName);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Mask support
//
//    public ProductNodeGroup<Mask> getMaskGroup() {
//        return getActualProduct().getMaskGroup();
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Vector data support
//
//    public ProductNodeGroup<VectorDataNode> getVectorDataGroup() {
//        return getActualProduct().getVectorDataGroup();
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Sample-coding support
//
//    public ProductNodeGroup<FlagCoding> getFlagCodingGroup() {
//        return getActualProduct().getFlagCodingGroup();
//    }
//
//    public ProductNodeGroup<IndexCoding> getIndexCodingGroup() {
//        return getActualProduct().getIndexCodingGroup();
//    }
//
//    public PlacemarkGroup getGcpGroup() {
//        return getActualProduct().getGcpGroup();
//    }
//
//    public synchronized PlacemarkGroup getPinGroup() {
//        return getActualProduct().getPinGroup();
//    }
//
//    //
//    //////////////////////////////////////////////////////////////////////////
//
//    public int getNumResolutionsMax() {
//        return getActualProduct().getNumResolutionsMax();
//    }
//
//    public void setNumResolutionsMax(int numResolutionsMax) {
//        getActualProduct().setNumResolutionsMax(numResolutionsMax);
//    }
//
//    public Term parseExpression(final String expression) throws ParseException {
//        return getActualProduct().parseExpression(expression);
//    }
//
//    public RasterDataNode[] getRefRasterDataNodes(String expression) throws ParseException {
//        return getActualProduct().getRefRasterDataNodes(expression);
//    }
//
//
//    //////////////////////////////////////////////////////////////////////////
//    // Visitor-Pattern support
//
//    @Override
//    public void acceptVisitor(final ProductVisitor visitor) {
//        getActualProduct().acceptVisitor(visitor);
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Product listener support
//
//    public boolean addProductNodeListener(final ProductNodeListener listener) {
//        return getActualProduct().addProductNodeListener(listener);
//    }
//
//    public void removeProductNodeListener(final ProductNodeListener listener) {
//        getActualProduct().removeProductNodeListener(listener);
//    }
//
//    public ProductNodeListener[] getProductNodeListeners() {
//        return getActualProduct().getProductNodeListeners();
//    }
//
//    protected boolean hasProductNodeListeners() {
//        return getActualProduct().getProductNodeListeners().length > 0;
//    }
//
////    protected void fireNodeChanged(ProductNode sourceNode, String propertyName, Object oldValue, Object newValue) {
////        fireEvent(sourceNode, propertyName, oldValue, newValue);
////    }
////
////    protected void fireNodeDataChanged(DataNode sourceNode) {
////        fireEvent(sourceNode, ProductNodeEvent.NODE_DATA_CHANGED, null);
////    }
////
////    protected void fireNodeAdded(ProductNode childNode, ProductNodeGroup nodeGroup) {
////        fireEvent(childNode, ProductNodeEvent.NODE_ADDED, nodeGroup);
////    }
////
////    protected void fireNodeRemoved(ProductNode childNode, ProductNodeGroup nodeGroup) {
////        fireEvent(childNode, ProductNodeEvent.NODE_REMOVED, nodeGroup);
////    }
////
////    private void fireEvent(final ProductNode sourceNode, int eventType, ProductNodeGroup nodeGroup) {
////        if (hasProductNodeListeners()) {
////            final ProductNodeEvent event = new ProductNodeEvent(sourceNode, eventType, nodeGroup);
////            fireEvent(event);
////        }
////    }
////
////    private void fireEvent(final ProductNode sourceNode, final String propertyName, Object oldValue, Object newValue) {
////        if (hasProductNodeListeners()) {
////            final ProductNodeEvent event = new ProductNodeEvent(sourceNode, propertyName, oldValue, newValue);
////            fireEvent(event);
////        }
////    }
////
////    private void fireEvent(final ProductNodeEvent event) {
////        fireEvent(event, listeners.toArray(new ProductNodeListener[listeners.size()]));
////    }
////
////    static void fireEvent(final ProductNodeEvent event, final ProductNodeListener[] productNodeListeners) {
////        for (ProductNodeListener listener : productNodeListeners) {
////            fireEvent(event, listener);
////        }
////    }
////
////    static void fireEvent(final ProductNodeEvent event, final ProductNodeListener listener) {
////        switch (event.getType()) {
////            case ProductNodeEvent.NODE_CHANGED:
////                listener.nodeChanged(event);
////                break;
////            case ProductNodeEvent.NODE_DATA_CHANGED:
////                listener.nodeDataChanged(event);
////                break;
////            case ProductNodeEvent.NODE_ADDED:
////                listener.nodeAdded(event);
////                break;
////            case ProductNodeEvent.NODE_REMOVED:
////                listener.nodeRemoved(event);
////                break;
////        }
////    }
//
//    public int getRefNo() {
//        return getActualProduct().getRefNo();
//    }
//
//    public void setRefNo(final int refNo) {
//        getActualProduct().setRefNo(refNo);
//    }
//
//    public void resetRefNo() {
//        getActualProduct().resetRefNo();
//    }
//
//    String getRefStr() {
//        return refStr;
//    }
//
//    public ProductManager getProductManager() {
//        return getActualProduct().getProductManager();
//    }
//
//    /**
//     * Sets the product manager for this product. Called by a {@code PropductManager} to set the product's
//     * ownership.
//     *
//     * @param productManager this product's manager, can be {@code null}
//     */
//    void setProductManager(final ProductManager productManager) {
//        this.productManager = productManager;
//    }
//
//    //////////////////////////////////////////////////////////////////////////
//    // Utilities
//
//    /**
//     * Tests if the given band arithmetic expression can be computed using this product.
//     *
//     * @param expression the mathematical expression
//     *
//     * @return true, if the band arithmetic is compatible with this product
//     *
//     * @see #isCompatibleBandArithmeticExpression(String, org.esa.snap.core.jexp.Parser)
//     */
//    public boolean isCompatibleBandArithmeticExpression(final String expression) {
//        return isCompatibleBandArithmeticExpression(expression, null);
//    }
//
//    /**
//     * Tests if the given band arithmetic expression can be computed using this product and a given expression parser.
//     *
//     * @param expression the band arithmetic expression
//     * @param parser     the expression parser to be used
//     *
//     * @return true, if the band arithmetic is compatible with this product
//     *
//     * @see #createBandArithmeticParser()
//     */
//    public boolean isCompatibleBandArithmeticExpression(final String expression, Parser parser) {
//        Guardian.assertNotNull("expression", expression);
//        if (containsBand(expression)) {
//            return true;
//        }
//        if (parser == null) {
//            parser = createBandArithmeticParser();
//        }
//        final Term term;
//        try {
//            term = parser.parse(expression);
//        } catch (ParseException e) {
//            return false;
//        }
//        // expression was empty
//        if (term == null) {
//            return false;
//        }
//
//        if (!BandArithmetic.areRastersEqualInSize(term)) {
//            return false;
//        }
//
//        final RasterDataSymbol[] termSymbols = BandArithmetic.getRefRasterDataSymbols(term);
//        for (final RasterDataSymbol termSymbol : termSymbols) {
//            final RasterDataNode refRaster = termSymbol.getRaster();
//            if (refRaster.getProduct() != this) {
//                return false;
//            }
//            if (termSymbol instanceof SingleFlagSymbol) {
//                final String[] flagNames = ((Band) refRaster).getFlagCoding().getFlagNames();
//                final String symbolName = termSymbol.getName();
//                final String flagName = symbolName.substring(symbolName.indexOf('.') + 1);
//                if (!StringUtils.containsIgnoreCase(flagNames, flagName)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//
//    /**
//     * Creates a parser for band arithmetic expressions.
//     * The parser created will use a namespace comprising all tie-point grids, bands and flags of this product.
//     *
//     * @return a parser for band arithmetic expressions for this product, never null
//     */
//    public Parser createBandArithmeticParser() {
//        final Namespace namespace = createBandArithmeticDefaultNamespace();
//        return new ParserImpl(namespace, false);
//    }
//
//    /**
//     * Creates a namespace to be used by parsers for band arithmetic expressions.
//     * The namespace created comprises all tie-point grids, bands and flags of this product.
//     *
//     * @return a namespace, never null
//     */
//    public WritableNamespace createBandArithmeticDefaultNamespace() {
//        return BandArithmetic.createDefaultNamespace(new Product[]{this}, 0);
//    }
//
//
//    /**
//     * Creates a subset of this product. The returned product represents a true spatial and spectral subset of this
//     * product, but it has not loaded any bands into memory. If name or desc are null or empty, the name and the
//     * description from this product was used.
//     *
//     * @param subsetDef the product subset definition
//     * @param name      the name for the new product
//     * @param desc      the description for the new product
//     *
//     * @return the product subset, or {@code null} if the product/subset combination is not valid
//     *
//     * @throws IOException if an I/O error occurs
//     */
//    public Product createSubset(final ProductSubsetDef subsetDef, final String name, final String desc) throws
//            IOException {
//        return ProductSubsetBuilder.createProductSubset(this, subsetDef, name, desc);
//    }
//
//    @Override
//    public void setModified(final boolean modified) {
//        final boolean oldState = isModified();
//        if (oldState != modified) {
//            super.setModified(modified);
//            if (!modified) {
//                bandGroup.setModified(false);
//                tiePointGridGroup.setModified(false);
//                maskGroup.setModified(false);
//                quicklookGroup.setModified(false);
//                vectorDataGroup.setModified(false);
//                flagCodingGroup.setModified(false);
//                indexCodingGroup.setModified(false);
//                getMetadataRoot().setModified(false);
//            }
//        }
//    }
//
//    /**
//     * Gets an estimated, raw storage size in bytes of this product node.
//     *
//     * @param subsetDef if not {@code null} the subset may limit the size returned
//     *
//     * @return the size in bytes.
//     */
//    @Override
//    public long getRawStorageSize(final ProductSubsetDef subsetDef) {
//        long size = 0;
//        for (int i = 0; i < getNumBands(); i++) {
//            size += getBandAt(i).getRawStorageSize(subsetDef);
//        }
//        for (int i = 0; i < getNumTiePointGrids(); i++) {
//            size += getTiePointGridAt(i).getRawStorageSize(subsetDef);
//        }
//        for (int i = 0; i < getFlagCodingGroup().getNodeCount(); i++) {
//            size += getFlagCodingGroup().get(i).getRawStorageSize(subsetDef);
//        }
//        for (int i = 0; i < getMaskGroup().getNodeCount(); i++) {
//            size += getMaskGroup().get(i).getRawStorageSize(subsetDef);
//        }
//        for (int i = 0; i < getQuicklookGroup().getNodeCount(); i++) {
//            size += getQuicklookGroup().get(i).getRawStorageSize(subsetDef);
//        }
//        size += getMetadataRoot().getRawStorageSize(subsetDef);
//        return size;
//    }
//
////    private static String extractProductName(File file) {
////        Guardian.assertNotNull("file", file);
////        String filename = file.getName();
////        int dotIndex = filename.indexOf('.');
////        if (dotIndex > -1) {
////            filename = filename.substring(0, dotIndex);
////        }
////        return filename;
////    }
//
//
//    /**
//     * Creates a string containing all available information at the given pixel position. The string returned is a line
//     * separated text with each line containing a key/value pair.
//     *
//     * @param pixelX the pixel X co-ordinate
//     * @param pixelY the pixel Y co-ordinate
//     *
//     * @return the info string at the given position
//     */
//    public String createPixelInfoString(final int pixelX, final int pixelY) {
//        final StringBuilder sb = new StringBuilder(1024);
//
//        sb.append("Product:\t");
//        sb.append(getName()).append("\n\n");
//
//        sb.append("Image-X:\t");
//        sb.append(pixelX);
//        sb.append("\tpixel\n");
//
//        sb.append("Image-Y:\t");
//        sb.append(pixelY);
//        sb.append("\tpixel\n");
//
//        if (getSceneGeoCoding() != null) {
//            final PixelPos pt = new PixelPos(pixelX + 0.5f, pixelY + 0.5f);
//            final GeoPos geoPos = getSceneGeoCoding().getGeoPos(pt, null);
//
//            sb.append("Longitude:\t");
//            sb.append(geoPos.getLonString());
//            sb.append("\tdegree\n");
//
//            sb.append("Latitude:\t");
//            sb.append(geoPos.getLatString());
//            sb.append("\tdegree\n");
//
//            if (getSceneGeoCoding() instanceof MapGeoCoding) {
//                final MapGeoCoding mapGeoCoding = (MapGeoCoding) getSceneGeoCoding();
//                final MapProjection mapProjection = mapGeoCoding.getMapInfo().getMapProjection();
//                final MapTransform mapTransform = mapProjection.getMapTransform();
//                final Point2D mapPoint = mapTransform.forward(geoPos, null);
//                final String mapUnit = mapProjection.getMapUnit();
//
//                sb.append("Map-X:\t");
//                sb.append(mapPoint.getX());
//                sb.append("\t").append(mapUnit).append("\n");
//
//                sb.append("Map-Y:\t");
//                sb.append(mapPoint.getY());
//                sb.append("\t").append(mapUnit).append("\n");
//            }
//        }
//
//        if (pixelX >= 0 && pixelX < getSceneRasterWidth()
//                && pixelY >= 0 && pixelY < getSceneRasterHeight()) {
//
//            sb.append("\n");
//
//            boolean haveSpectralBand = false;
//            for (final Band band : getBands()) {
//                if (band.getSpectralWavelength() > 0.0) {
//                    haveSpectralBand = true;
//                    break;
//                }
//            }
//
//            if (haveSpectralBand) {
//                sb.append("BandName\tWavelength\tUnit\tBandwidth\tUnit\tValue\tUnit\tSolar Flux\tUnit\n");
//            } else {
//                sb.append("BandName\tValue\tUnit\n");
//            }
//            for (final Band band : getBands()) {
//                sb.append(band.getName());
//                sb.append(":\t");
//                if (band.getSpectralWavelength() > 0.0) {
//                    sb.append(band.getSpectralWavelength());
//                    sb.append("\t");
//                    sb.append("nm");
//                    sb.append("\t");
//                    sb.append(band.getSpectralBandwidth());
//                    sb.append("\t");
//                    sb.append("nm");
//                    sb.append("\t");
//                } else {
//                    if (haveSpectralBand) {
//                        sb.append("\t");
//                        sb.append("\t");
//                        sb.append("\t");
//                        sb.append("\t");
//                    }
//                }
//                sb.append(band.getPixelString(pixelX, pixelY));
//                sb.append("\t");
//                if (band.getUnit() != null) {
//                    sb.append(band.getUnit());
//                }
//                sb.append("\t");
//                final float solarFlux = band.getSolarFlux();
//                if (solarFlux > 0.0) {
//                    sb.append(solarFlux);
//                    sb.append("\t");
//                    sb.append("mW/(m^2*nm)");
//                    sb.append("\t");
//                }
//                sb.append("\n");
//            }
//
//            sb.append("\n");
//            for (int i = 0; i < getNumTiePointGrids(); i++) {
//                final TiePointGrid grid = getTiePointGridAt(i);
//                if (grid.hasRasterData()) {
//                    sb.append(grid.getName());
//                    sb.append(":\t");
//                    sb.append(grid.getPixelString(pixelX, pixelY));
//                    if (grid.getUnit() != null) {
//                        sb.append("\t");
//                        sb.append(grid.getUnit());
//                    }
//
//                    sb.append("\n");
//                }
//            }
//
//            for (int i = 0; i < getNumBands(); i++) {
//                final Band band = getBandAt(i);
//                final FlagCoding flagCoding = band.getFlagCoding();
//                if (flagCoding != null) {
//                    boolean ioException = false;
//                    final int[] flags = new int[1];
//                    if (band.hasRasterData()) {
//                        flags[0] = band.getPixelInt(pixelX, pixelY);
//                    } else {
//                        try {
//                            band.readPixels(pixelX, pixelY, 1, 1, flags, ProgressMonitor.NULL);
//                        } catch (IOException e) {
//                            ioException = true;
//                        }
//                    }
//                    sb.append("\n");
//                    if (ioException) {
//                        sb.append(RasterDataNode.IO_ERROR_TEXT);
//                    } else {
//                        for (int j = 0; j < flagCoding.getNumAttributes(); j++) {
//                            final MetadataAttribute flagAttr = flagCoding.getAttributeAt(j);
//                            final int mask = flagAttr.getData().getElemInt();
//                            final boolean flagSet = (flags[0] & mask) == mask;
//                            sb.append(band.getName());
//                            sb.append(".");
//                            sb.append(flagAttr.getName());
//                            sb.append(":\t");
//                            sb.append(flagSet ? "true" : "false");
//                            sb.append("\n");
//                        }
//                    }
//                }
//            }
//        }
//
//        return sb.toString();
//    }
//
//    /**
//     * @return All removed child nodes. Array may be empty.
//     */
//    public ProductNode[] getRemovedChildNodes() {
//        final ArrayList<ProductNode> removedNodes = new ArrayList<>();
//        removedNodes.addAll(bandGroup.getRemovedNodes());
//        removedNodes.addAll(flagCodingGroup.getRemovedNodes());
//        removedNodes.addAll(indexCodingGroup.getRemovedNodes());
//        removedNodes.addAll(tiePointGridGroup.getRemovedNodes());
//        removedNodes.addAll(maskGroup.getRemovedNodes());
//        removedNodes.addAll(quicklookGroup.getRemovedNodes());
//        removedNodes.addAll(vectorDataGroup.getRemovedNodes());
//        return removedNodes.toArray(new ProductNode[removedNodes.size()]);
//    }
//
//
//    private void checkGeoCoding(final GeoCoding geoCoding) {
//        if (geoCoding instanceof TiePointGeoCoding) {
//            final TiePointGeoCoding gc = (TiePointGeoCoding) geoCoding;
//            Guardian.assertSame("gc.getLatGrid()", gc.getLatGrid(), getTiePointGrid(gc.getLatGrid().getName()));
//            Guardian.assertSame("gc.getLonGrid()", gc.getLonGrid(), getTiePointGrid(gc.getLonGrid().getName()));
//        } else if (geoCoding instanceof MapGeoCoding) {
//            final MapGeoCoding gc = (MapGeoCoding) geoCoding;
//            final MapInfo mapInfo = gc.getMapInfo();
//            Guardian.assertNotNull("mapInfo", mapInfo);
//            Guardian.assertEquals("mapInfo.getSceneWidth()", mapInfo.getSceneWidth(), getSceneRasterWidth());
//            Guardian.assertEquals("mapInfo.getSceneHeight()", mapInfo.getSceneHeight(), getSceneRasterHeight());
//        }
//    }
//
//    /**
//     * Checks whether or not this product can be orthorectified.
//     *
//     * @return true if {@link Band#canBeOrthorectified()} returns true for all bands, false otherwise
//     */
//    public boolean canBeOrthorectified() {
//        for (int i = 0; i < getNumBands(); i++) {
//            if (!getBandAt(i).canBeOrthorectified()) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private String getSuitableMaskDefDescription(final String expr) {
//
//        if (StringUtils.isNullOrEmpty(expr)) {
//            return null;
//        }
//
//        final Term term;
//        try {
//            term = BandArithmetic.parseExpression(expr, new Product[]{this}, 0);
//        } catch (ParseException e) {
//            return null;
//        }
//
//        if (term instanceof Term.Ref) {
//            return getSuitableMaskDefDescription((Term.Ref) term);
//        }
//
//        if (term instanceof Term.NotB) {
//            final Term.NotB notTerm = ((Term.NotB) term);
//            final Term arg = notTerm.getArgs()[0];
//            if (arg instanceof Term.Ref) {
//                final String description = getSuitableMaskDefDescription((Term.Ref) arg);
//                if (description != null) {
//                    return "Not " + description;
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private String getSuitableMaskDefDescription(Term.Ref ref) {
//        String description = null;
//        final String symbolName = ref.getSymbol().getName();
//        if (isFlagSymbol(symbolName)) {
//            final String[] strings = StringUtils.split(symbolName, new char[]{'.'}, true);
//            final String nodeName = strings[0];
//            final String flagName = strings[1];
//            final RasterDataNode rasterDataNode = getRasterDataNode(nodeName);
//            if (rasterDataNode instanceof Band) {
//                final FlagCoding flagCoding = ((Band) rasterDataNode).getFlagCoding();
//                if (flagCoding != null) {
//                    final MetadataAttribute attribute = flagCoding.getAttribute(flagName);
//                    if (attribute != null) {
//                        description = attribute.getDescription();
//                    }
//                }
//            }
//        } else {
//            final RasterDataNode rasterDataNode = getRasterDataNode(symbolName);
//            if (rasterDataNode != null) {
//                description = rasterDataNode.getDescription();
//            }
//        }
//        return description;
//    }
//
//    private static boolean isFlagSymbol(final String symbolName) {
//        return symbolName.indexOf('.') != -1;
//    }
//
//
//    /**
//     * Gets the preferred tile size which may be used for a the {@link java.awt.image.RenderedImage rendered image}
//     * created for a {@link RasterDataNode} of this product.
//     *
//     * @return the preferred tile size, may be {@code null} if not specified
//     *
//     * @see RasterDataNode#getSourceImage()
//     * @see RasterDataNode#setSourceImage(java.awt.image.RenderedImage)
//     */
//    public Dimension getPreferredTileSize() {
//        return preferredTileSize;
//    }
//
//    /**
//     * Sets the preferred tile size which may be used for a the {@link java.awt.image.RenderedImage rendered image}
//     * created for a {@link RasterDataNode} of this product.
//     *
//     * @param tileWidth  the preferred tile width
//     * @param tileHeight the preferred tile height
//     *
//     * @see #setPreferredTileSize(java.awt.Dimension)
//     */
//    public void setPreferredTileSize(int tileWidth, int tileHeight) {
//        setPreferredTileSize(new Dimension(tileWidth, tileHeight));
//    }
//
//    /**
//     * Sets the preferred tile size which may be used for a the {@link java.awt.image.RenderedImage rendered image}
//     * created for a {@link RasterDataNode} of this product.
//     *
//     * @param preferredTileSize the preferred tile size, may be {@code null} if not specified
//     *
//     * @see RasterDataNode#getSourceImage()
//     * @see RasterDataNode#setSourceImage(java.awt.image.RenderedImage)
//     */
//    public void setPreferredTileSize(Dimension preferredTileSize) {
//        this.preferredTileSize = preferredTileSize;
//    }
//
//    /**
//     * Returns the names of all flags of all flag datasets contained this product.
//     * <p>A flag name contains the dataset (a band of this product) and the actual flag name as defined in the
//     * flag-coding associated with the dataset. The general format for the flag name strings returned is therefore
//     * <code>"<i>dataset</i>.<i>flag_name</i>"</code>.
//     *
//     * <p>The method is used to find out which flags a product has in order to use them in bit-mask expressions.
//     *
//     * @return the array of all flag names. If this product does not support flags, an empty array is returned, but
//     * never {@code null}.
//     *
//     * @see #parseExpression(String)
//     */
//    public String[] getAllFlagNames() {
//        final List<String> l = new ArrayList<>(32);
//        for (int i = 0; i < getNumBands(); i++) {
//            final Band band = getBandAt(i);
//            if (band.getFlagCoding() != null) {
//                for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
//                    final MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
//                    l.add(band.getName() + "." + attribute.getName());
//                }
//            }
//        }
//        final String[] flagNames = new String[l.size()];
//        for (int i = 0; i < flagNames.length; i++) {
//            flagNames[i] = l.get(i);
//        }
//        l.clear();
//        return flagNames;
//    }
//
//    /**
//     * Gets the auto-grouping applicable to product nodes contained in this product.
//     *
//     * @return The auto-grouping or {@code null}.
//     *
//     * @since BEAM 4.8
//     */
//    public Product.AutoGrouping getAutoGrouping() {
//        return this.autoGrouping;
//    }
//
//    /**
//     * Sets the auto-grouping applicable to product nodes contained in this product.
//     *
//     * @param autoGrouping The auto-grouping or {@code null}.
//     *
//     * @since BEAM 4.8
//     */
//    public void setAutoGrouping(Product.AutoGrouping autoGrouping) {
//        Product.AutoGrouping old = this.autoGrouping;
//        if (!ObjectUtils.equalObjects(old, autoGrouping)) {
//            this.autoGrouping = autoGrouping;
//            fireProductNodeChanged("autoGrouping", old, this.autoGrouping);
//        }
//    }
//
//    /**
//     * Sets the auto-grouping applicable to product nodes contained in this product.
//     * A given {@code pattern} parameter is a textual representation of the auto-grouping.
//     * The syntax for the pattern is:
//     * <pre>
//     * pattern    :=  &lt;groupPath&gt; {':' &lt;groupPath&gt;} | "" (empty string)
//     * groupPath  :=  &lt;groupName&gt; {'/' &lt;groupName&gt;}
//     * groupName  :=  any non-empty string without characters ':' and '/'
//     * </pre>
//     * An example for {@code pattern} applicable to Envisat AATSR data is
//     * <pre>
//     * nadir/reflec:nadir/btemp:fward/reflec:fward/btemp:nadir:fward
//     * </pre>
//     *
//     * @param pattern The auto-grouping pattern.
//     *
//     * @since BEAM 4.8
//     */
//    public void setAutoGrouping(String pattern) {
//        Assert.notNull(pattern, "text");
//        setAutoGrouping(Product.AutoGroupingImpl.parse(pattern));
//    }
//
//    /**
//     * Creates a new mask with the given name and image type and adds it to this product and returns it.
//     * The new mask's samples are computed from the given image type.
//     *
//     * @param maskName  the new mask's name
//     * @param imageType the image data type used to compute the mask samples
//     *
//     * @return the new mask which has just been added
//     *
//     * @since BEAM 4.10
//     */
//    public Mask addMask(String maskName, Mask.ImageType imageType) {
//        final Mask mask = new Mask(maskName, getSceneRasterWidth(), getSceneRasterHeight(), imageType);
//        addMask(mask);
//        return mask;
//    }
//
//    /**
//     * Creates a new mask using a band arithmetic expression
//     * and adds it to this product and returns it.
//     *
//     * @param maskName     the new mask's name
//     * @param expression   the band arithmetic expression
//     * @param description  the mask's description
//     * @param color        the display color
//     * @param transparency the display transparency
//     *
//     * @return the new mask which has just been added
//     *
//     * @throws IllegalArgumentException when the expression references rasters of different sizes
//     * @since BEAM 4.10
//     */
//    public Mask addMask(String maskName, String expression, String description, Color color, double transparency) {
//        RasterDataNode[] refRasters = new RasterDataNode[0];
//        try {
//            final ProductManager productManager = getProductManager();
//            Product[] products = new Product[]{this};
//            int productIndex = 0;
//            if (productManager != null) {
//                products = productManager.getProducts();
//                productIndex = productManager.getProductIndex(this);
//            }
//            if (BandArithmetic.areRastersEqualInSize(products, productIndex, expression)) {
//                refRasters = BandArithmetic.getRefRasters(expression, products, productIndex);
//            } else {
//                throw new IllegalArgumentException("Expression must not reference rasters of different sizes");
//            }
//        } catch (ParseException e) {
//            Logger.getLogger(Product.class.getName()).warning(String.format("Adding invalid expression '%s' to product",
//                    expression));
//        }
//        Mask mask;
//        if (refRasters.length == 0) {
//            mask = Mask.BandMathsType.create(maskName, description,
//                    getSceneRasterWidth(), getSceneRasterHeight(),
//                    expression, color, transparency);
//        } else {
//            final RasterDataNode refRaster = refRasters[0];
//            mask = Mask.BandMathsType.create(maskName, description,
//                    refRaster.getRasterWidth(),
//                    refRaster.getRasterHeight(),
//                    expression, color, transparency);
//            mask.setGeoCoding(refRaster.getGeoCoding());
//        }
//        addMask(mask);
//        return mask;
//    }
//
//    /**
//     * Creates a new mask based on the geometries contained in a vector data node,
//     * adds it to this product and returns it.
//     *
//     * @param maskName       the new mask's name
//     * @param vectorDataNode the vector data node
//     * @param description    the mask's description
//     * @param color          the display color
//     * @param transparency   the display transparency
//     *
//     * @return the new mask which has just been added
//     *
//     * @since BEAM 4.10
//     */
//    public Mask addMask(String maskName,
//                        VectorDataNode vectorDataNode,
//                        String description,
//                        Color color,
//                        double transparency) {
//        final Mask mask = new Mask(maskName,
//                getSceneRasterWidth(),
//                getSceneRasterHeight(),
//                Mask.VectorDataType.INSTANCE);
//        Mask.VectorDataType.setVectorData(mask, vectorDataNode);
//        mask.setDescription(description);
//        mask.setImageColor(color);
//        mask.setImageTransparency(transparency);
//        addMask(mask);
//        return mask;
//    }
//
//    /**
//     * Creates a new mask based on the geometries contained in a vector data node,
//     * adds it to this product and returns it.
//     *
//     * @param maskName                the new mask's name
//     * @param vectorDataNode          the vector data node
//     * @param description             the mask's description
//     * @param color                   the display color
//     * @param transparency            the display transparency
//     * @param prototypeRasterDataNode a raster data node used to serve as a prototypeRasterDataNode for image layout and geo-coding. May be {@code null}.
//     *
//     * @return the new mask which has just been added
//     *
//     * @since SNAP 2.0
//     */
//    public Mask addMask(String maskName,
//                        VectorDataNode vectorDataNode,
//                        String description,
//                        Color color,
//                        double transparency,
//                        RasterDataNode prototypeRasterDataNode) {
//        final Mask mask = new Mask(maskName,
//                prototypeRasterDataNode != null ? prototypeRasterDataNode.getRasterWidth() : getSceneRasterWidth(),
//                prototypeRasterDataNode != null ? prototypeRasterDataNode.getRasterHeight() : getSceneRasterHeight(),
//                Mask.VectorDataType.INSTANCE);
//        Mask.VectorDataType.setVectorData(mask, vectorDataNode);
//        mask.setDescription(description);
//        mask.setImageColor(color);
//        mask.setImageTransparency(transparency);
//        if (prototypeRasterDataNode != null) {
//            ProductUtils.copyImageGeometry(prototypeRasterDataNode, mask, false);
//        }
//        addMask(mask);
//        return mask;
//    }
//
//    /**
//     * Adds the given mask to this product.
//     *
//     * @param mask the mask to be added, must not be {@code null}
//     */
//    public void addMask(Mask mask) {
//        getMaskGroup().add(mask);
//    }
//
//    /**
//     * AutoGrouping can be used by an application to auto-group a long list of product nodes (e.g. bands)
//     * as a tree of product nodes.
//     *
//     * @since BEAM 4.8
//     */
//    public interface AutoGrouping extends List<String[]> {
//
//        static Product.AutoGrouping parse(String text) {
//            return Product.AutoGroupingImpl.parse(text);
//        }
//
//        /**
//         * Gets the index of the first group path that matches the given name.
//         *
//         * @param name A product node name.
//         *
//         * @return The index of the group path or {@code -1} if no group path matches the given name.
//         */
//        int indexOf(String name);
//    }
//
//    private static class AutoGroupingImpl extends AbstractList<String[]> implements Product.AutoGrouping {
//
//        private static final String GROUP_SEPARATOR = "/";
//        private static final String PATH_SEPARATOR = ":";
//
//        private final Product.AutoGroupingPath[] autoGroupingPaths;
//        private final Product.AutoGroupingImpl.Index[] indexes;
//
//        private AutoGroupingImpl(String[][] inputPaths) {
//            autoGroupingPaths = new Product.AutoGroupingPath[inputPaths.length];
//            this.indexes = new Product.AutoGroupingImpl.Index[inputPaths.length];
//            for (int i = 0; i < inputPaths.length; i++) {
//                final Product.AutoGroupingPath autoGroupingPath = new Product.AutoGroupingPath(inputPaths[i]);
//                autoGroupingPaths[i] = autoGroupingPath;
//                indexes[i] = new Product.AutoGroupingImpl.Index(autoGroupingPath, i);
//            }
//            Arrays.sort(indexes, (o1, o2) -> {
//                final String[] o1InputPath = o1.path.getInputPath();
//                final String[] o2InputPath = o2.path.getInputPath();
//                int index = 0;
//
//                while (index < o1InputPath.length && index < o2InputPath.length) {
//                    final String currentO1InputPathString = o1InputPath[index];
//                    final String currentO2InputPathString = o2InputPath[index];
//                    if (currentO1InputPathString.length() != currentO2InputPathString.length()) {
//                        return currentO2InputPathString.length() - currentO1InputPathString.length();
//                    }
//                    index++;
//                }
//                if (o1InputPath.length != o2InputPath.length) {
//                    return o2InputPath.length - o1InputPath.length;
//                }
//                return o2InputPath[0].compareTo(o1InputPath[0]);
//            });
//        }
//
//        @Override
//        public int indexOf(String name) {
//            for (Product.AutoGroupingImpl.Index index : indexes) {
//                final int i = index.index;
//                if (index.path.contains(name)) {
//                    return i;
//                }
//            }
//            return -1;
//        }
//
//        @Override
//        public String[] get(int index) {
//            return autoGroupingPaths[index].getInputPath();
//        }
//
//        @Override
//        public int size() {
//            return autoGroupingPaths.length;
//        }
//
//        public static Product.AutoGrouping parse(String text) {
//            List<String[]> pathLists = new ArrayList<>();
//            if (StringUtils.isNotNullAndNotEmpty(text)) {
//                String[] pathTexts = StringUtils.toStringArray(text, PATH_SEPARATOR);
//                for (String pathText : pathTexts) {
//                    final String[] subPaths = StringUtils.toStringArray(pathText, GROUP_SEPARATOR);
//                    final ArrayList<String> subPathsList = new ArrayList<>();
//                    for (String subPath : subPaths) {
//                        if (StringUtils.isNotNullAndNotEmpty(subPath)) {
//                            subPathsList.add(subPath);
//                        }
//                    }
//                    if (!subPathsList.isEmpty()) {
//                        pathLists.add(subPathsList.toArray(new String[subPathsList.size()]));
//                    }
//                }
//                if (pathLists.isEmpty()) {
//                    return null;
//                }
//                return new Product.AutoGroupingImpl(pathLists.toArray(new String[pathLists.size()][]));
//            } else {
//                return null;
//            }
//        }
//
//        public String format() {
//            if (autoGroupingPaths.length > 0) {
//                StringBuilder sb = new StringBuilder();
//                for (int i = 0; i < autoGroupingPaths.length; i++) {
//                    if (i > 0) {
//                        sb.append(PATH_SEPARATOR);
//                    }
//                    String[] path = autoGroupingPaths[i].getInputPath();
//                    for (int j = 0; j < path.length; j++) {
//                        if (j > 0) {
//                            sb.append(GROUP_SEPARATOR);
//                        }
//                        sb.append(path[j]);
//                    }
//                }
//                return sb.toString();
//            } else {
//                return "";
//            }
//        }
//
//        @Override
//        public String toString() {
//            return format();
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) {
//                return true;
//            } else if (o instanceof Product.AutoGrouping) {
//                Product.AutoGrouping other = (Product.AutoGrouping) o;
//                if (other.size() != size()) {
//                    return false;
//                }
//                for (int i = 0; i < autoGroupingPaths.length; i++) {
//                    String[] path = autoGroupingPaths[i].getInputPath();
//                    if (!ObjectUtils.equalObjects(path, other.get(i))) {
//                        return false;
//                    }
//                }
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        @Override
//        public int hashCode() {
//            int code = 0;
//            for (Product.AutoGroupingPath autoGroupingPath : autoGroupingPaths) {
//                String[] path = autoGroupingPath.getInputPath();
//                code += Arrays.hashCode(path);
//            }
//            return code;
//        }
//
//
//        private static class Index {
//
//            final int index;
//            final Product.AutoGroupingPath path;
//
//            private Index(Product.AutoGroupingPath path, int index) {
//                this.path = path;
//                this.index = index;
//            }
//
//        }
//    }
//
//    private static class AutoGroupingPath {
//
//        private final String[] groups;
//        private final Product.Entry[] entries;
//
//        AutoGroupingPath(String[] groups) {
//            this.groups = groups;
//            entries = new Product.Entry[groups.length];
//            for (int i = 0; i < groups.length; i++) {
//                if (groups[i].contains("*") || groups[i].contains("?")) {
//                    entries[i] = new Product.WildCardEntry(groups[i]);
//                } else {
//                    entries[i] = new Product.EntryImpl(groups[i]);
//                }
//            }
//        }
//
//        boolean contains(String name) {
//            for (Product.Entry entry : entries) {
//                if (!entry.matches(name)) {
//                    return false;
//                }
//            }
//            return true;
//        }
//
//        String[] getInputPath() {
//            return groups;
//        }
//
//    }
//
//    interface Entry {
//
//        boolean matches(String name);
//
//    }
//
//    private static class EntryImpl implements Product.Entry {
//
//        private final String group;
//
//        EntryImpl(String group) {
//            this.group = group;
//        }
//
//
//        @Override
//        public boolean matches(String name) {
//            return name.contains(group);
//        }
//    }
//
//    private static class WildCardEntry implements Product.Entry {
//
//        private final WildcardMatcher wildcardMatcher;
//
//        WildCardEntry(String group) {
//            wildcardMatcher = new WildcardMatcher(group);
//        }
//
//        @Override
//        public boolean matches(String name) {
//            return wildcardMatcher.matches(name);
//        }
//    }
//
//    /////////////////////////////////////////////////////////////////////////
//    // Deprecated API
//
//    /**
//     * Gets a multi-level mask image for the given band maths expression and an optional associated raster.
//     * The associated raster is used to infer the target mask's image (tile) layout.
//     * <p>
//     * If the associated raster is {@code null}, the mask's tile size is
//     * this product's {@link #getPreferredTileSize() preferred tile size} (if any) while other image layout settings
//     * are derived from {@link #createMultiLevelModel()}.
//     *
//     * @param expression       The expression
//     * @param associatedRaster The associated raster or {@code null}.
//     *
//     * @return A multi-level mask image.
//     */
//    public MultiLevelImage getMaskImage(String expression, RasterDataNode associatedRaster) {
//        synchronized (this) {
//            if (maskCache == null) {
//                maskCache = new HashMap<>();
//            }
//            WeakReference<MultiLevelImage> maskImageRef = maskCache.get(expression);
//            MultiLevelImage maskImage = null;
//            if (maskImageRef != null) {
//                maskImage = maskImageRef.get();
//            }
//            if (maskImage == null) {
//                maskImage = createMaskImage(expression, associatedRaster);
//                maskCache.put(expression, new WeakReference<>(maskImage));
//            }
//            return maskImage;
//        }
//    }
//
//}
