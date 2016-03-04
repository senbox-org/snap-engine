package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.insitu.server.InsituDatasetDescr;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerRunnable;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.ProgressHandleMonitor;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

class InsituClientModel {


    static final InsituServerSpi NO_SELECTION_SERVER_SPI = new NoSelectionInsituServerSpi();
    static final String PROPERTY_START_DATE = "startDate";
    static final String PROPERTY_STOP_DATE = "stopDate";
    static final String PROPERTY_MIN_LON = "minLon";
    static final String PROPERTY_MAX_LON = "maxLon";
    static final String PROPERTY_MIN_LAT = "minLat";
    static final String PROPERTY_MAX_LAT = "maxLat";

    private final DefaultComboBoxModel<InsituServerSpi> insituServerModel;
    private final DefaultListModel<InsituDatasetDescr> datasetModel;
    private final ListSelectionModel datasetSelectionModel;
    private final DefaultListModel<InsituParameter> parameterModel;
    private final DefaultListSelectionModel parameterSelectionModel;
    private final DefaultListModel<Product> productListModel;
    private final DefaultListSelectionModel productSelectionModel;
    private Date startDate;
    private Date stopDate;
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;

    private InsituServer selectedServer;
    private PMListener productManagerListener;
    private PropertyChangeSupport changeSupport;

    public InsituClientModel() {
        final Set<InsituServerSpi> allRegisteredServers = InsituServerRegistry.getInstance().getAllRegisteredServers();
        InsituServerSpi[] servers = allRegisteredServers.toArray(new InsituServerSpi[0]);
        insituServerModel = new DefaultComboBoxModel<>(servers);
        insituServerModel.insertElementAt(NO_SELECTION_SERVER_SPI, 0);
        insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
        insituServerModel.addListDataListener(new ServerListener());

        datasetModel = new DefaultListModel<>();
        datasetSelectionModel = new DefaultListSelectionModel();
        datasetSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        datasetSelectionModel.addListSelectionListener(new DatasetListSelectionListener());

        parameterModel = new DefaultListModel<>();
        parameterSelectionModel = new DefaultListSelectionModel();

        productListModel = new DefaultListModel<>();
        productSelectionModel = new DefaultListSelectionModel();
        productSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productSelectionModel.addListSelectionListener(new ProductListSelectionListener());
        final ProductManager productManager = SnapApp.getDefault().getProductManager();
        Product[] products = productManager.getProducts();
        for (Product product : products) {
            productListModel.addElement(product);
        }
        productManagerListener = new PMListener(productManager);
        productManager.addListener(productManagerListener);
        TimeSpan timeSpan = TimeSpan.create(Collections.emptyList());
        startDate = timeSpan.startDate;
        stopDate = timeSpan.stopDate;
        MinMaxGeoCoordinates minMaxGeoCoordinates = MinMaxGeoCoordinates.create(Collections.emptyList());
        minLon = minMaxGeoCoordinates.getMinLon();
        maxLon = minMaxGeoCoordinates.getMaxLon();
        minLat = minMaxGeoCoordinates.getMinLat();
        maxLat = minMaxGeoCoordinates.getMaxLat();

        changeSupport = new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

    public DefaultComboBoxModel<InsituServerSpi> getInsituServerModel() {
        return insituServerModel;
    }

    public InsituServerSpi getSelectedServerSpi() {
        return (InsituServerSpi) insituServerModel.getSelectedItem();
    }

    public DefaultListModel<InsituDatasetDescr> getDatasetModel() {
        return datasetModel;
    }

    public ListSelectionModel getDatasetSelectionModel() {
        return datasetSelectionModel;
    }

    public InsituDatasetDescr getSelectedDataset() {
        if(!datasetSelectionModel.isSelectionEmpty()) {
            final int selectionIndex = datasetSelectionModel.getLeadSelectionIndex();
            return datasetModel.get(selectionIndex);
        }
        return null;
    }

    public DefaultListModel<InsituParameter> getParameterModel() {
        return parameterModel;
    }

    public DefaultListSelectionModel getParameterSelectionModel() {
        return parameterSelectionModel;
    }

    public List<InsituParameter> getSelectedParameters() {
        return Utils.getSelectedItems(parameterModel, parameterSelectionModel);
    }

    public DefaultListModel<Product> getProductListModel() {
        return productListModel;
    }

    public DefaultListSelectionModel getProductSelectionModel() {
        return productSelectionModel;
    }

    public List<Product> getSelectedProducts() {
        return Utils.getSelectedItems(productListModel, productSelectionModel);
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        if (this.startDate.equals(startDate)) {
            return;
        }
        Date oldValue = this.startDate;
        this.startDate = startDate;
        changeSupport.firePropertyChange(PROPERTY_START_DATE, oldValue, startDate);
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        if (this.stopDate.equals(stopDate)) {
            return;
        }
        Date oldValue = this.stopDate;
        this.stopDate = stopDate;
        changeSupport.firePropertyChange(PROPERTY_STOP_DATE, oldValue, stopDate);
    }

    public double getMinLon() {
        return minLon;
    }

    public void setMinLon(double minLon) {
        if (this.minLon == minLon) {
            return;
        }
        double oldValue = this.minLon;
        this.minLon = minLon;
        changeSupport.firePropertyChange(PROPERTY_MIN_LON, oldValue, minLon);
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        if (this.maxLon == maxLon) {
            return;
        }
        double oldValue = this.maxLon;
        this.maxLon = maxLon;
        changeSupport.firePropertyChange(PROPERTY_MAX_LON, oldValue, maxLon);
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        if (this.minLat == minLat) {
            return;
        }
        double oldValue = this.minLat;
        this.minLat = minLat;
        changeSupport.firePropertyChange(PROPERTY_MIN_LAT, oldValue, minLat);

    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        if (this.maxLat == maxLat) {
            return;
        }
        double oldValue = this.maxLat;
        this.maxLat = maxLat;
        changeSupport.firePropertyChange(PROPERTY_MAX_LAT, oldValue, maxLat);
    }

    static Calendar createUtcCalendar() {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        return utcCalendar;
    }

    public void dispose() {
        SnapApp.getDefault().getProductManager().removeListener(productManagerListener);
    }

    private static class NoSelectionInsituServerSpi implements InsituServerSpi {

        @Override
        public String getName() {
            return "<NO_SERVER_CURRENTLY_SELECTED>";
        }

        @Override
        public String getDescription() {
            return "Please select one of the available in-situ server";
        }

        @Override
        public InsituServer createServer() throws InsituServerException {
            return new InsituServer() {
                @Override
                public String getName() {
                    return "NO_SERVER";
                }

                @Override
                public InsituResponse query(InsituQuery query) throws InsituServerException {
                    return InsituResponse.EMPTY_RESPONSE;
                }
            };
        }

    }

    private class ServerListener implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            // don't care for add
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            // don't care for remove
        }

        @Override
        public void contentsChanged(ListDataEvent event) {
            if (event.getIndex0() == -1 && event.getIndex1() == -1) {
                // selection changed if both indices are -1
                final InsituServerSpi insituServerSpi = getSelectedServerSpi();
                try {
                    selectedServer = insituServerSpi.createServer();
                    updateDatasetModel(selectedServer);
                    updateParameterModel(selectedServer);
                } catch (Exception e) {
                    insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
                    throw new IllegalStateException("Could not create server instance for server '" + insituServerSpi.getName() + "'", e);
                }
            }
        }
    }

    private void updateDatasetModel(InsituServer server) throws InsituServerException {
        getDatasetModel().clear();
        InsituQuery query = new InsituQuery().subject(InsituQuery.SUBJECT.DATASETS);
        InsituServerRunnable runnable = new InsituServerRunnable(server, query);
        InsituServer.runWithProgress(runnable);

        runnable.getResponse().getDatasetDescriptions().forEach(datasetModel::addElement);
    }

    private void updateParameterModel(InsituServer server) throws InsituServerException {
        getParameterModel().clear();
        InsituQuery query = new InsituQuery().subject(InsituQuery.SUBJECT.PARAMETERS);
        final InsituDatasetDescr datasetDescr = getSelectedDataset();
        if (datasetDescr != null) {
            query.dataset(datasetDescr.getName());
        }
        final InsituResponse insituResponse = server.query(query);
        insituResponse.getParameters().forEach(parameterModel::addElement);
    }

    private class DatasetListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent event) {
            try {
                if (selectedServer != null) {
                    updateParameterModel(selectedServer);
                }
            } catch (InsituServerException e) {
                SnapApp.getDefault().handleError("Failed to retrieve metadata from server", e);
            }
        }
    }

    private class PMListener implements ProductManager.Listener {

        private final ProductManager productManager;

        public PMListener(ProductManager productManager) {
            this.productManager = productManager;
        }

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();
            final int productIndex = productManager.getProductIndex(product);
            productListModel.add(productIndex, product);
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            final Product product = event.getProduct();
            final int productIndex = productManager.getProductIndex(product);
            productListModel.remove(productIndex);
        }
    }

    private class ProductListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) {
                return;
            }
            ProgressHandleMonitor handle = ProgressHandleMonitor.create("In-Situ Data Access");
            Runnable runnable = () -> updateTemporalAndSpatialBounds(handle);
            Utils.runWithProgress(runnable, handle);
        }

        private void updateTemporalAndSpatialBounds(ProgressMonitor pm) {
            pm.beginTask("Computing temporal and spatial bounds", 2);
            try {
                final List<Product> products = getSelectedProducts();

                TimeSpan timeSpan = TimeSpan.create(products);
                pm.worked(1);
                MinMaxGeoCoordinates minMaxGeoCoordinates = MinMaxGeoCoordinates.create(products);
                pm.worked(1);

                setStartDate(timeSpan.getStartDate());
                setStopDate(timeSpan.getStopDate());
                setMinLat(minMaxGeoCoordinates.getMinLat());
                setMaxLat(minMaxGeoCoordinates.getMaxLat());
                setMinLon(minMaxGeoCoordinates.getMinLon());
                setMaxLon(minMaxGeoCoordinates.getMaxLon());
            } finally {
                pm.done();
            }
        }
    }

    static class MinMaxGeoCoordinates {

        private double minLat;
        private double maxLat;
        private double minLon;
        private double maxLon;

        private MinMaxGeoCoordinates() {
        }

        public double getMinLat() {
            return minLat;
        }

        public double getMaxLat() {
            return maxLat;
        }

        public double getMinLon() {
            return minLon;
        }

        public double getMaxLon() {
            return maxLon;
        }

        public static MinMaxGeoCoordinates create(List<Product> products) {
            MinMaxGeoCoordinates coordinates = new MinMaxGeoCoordinates();
            coordinates.minLat = 90.0;
            coordinates.maxLat = -90.0;
            coordinates.minLon = 180.0;
            coordinates.maxLon = -180.0;
            for (Product product : products) {
                if (product.getSceneGeoCoding() != null) {
                    final GeoPos[] corners = createCornerCoordinates(product);
                    for (GeoPos corner : corners) {
                        coordinates.minLat = Math.min(coordinates.minLat, corner.getLat());
                        coordinates.maxLat = Math.max(coordinates.maxLat, corner.getLat());
                        coordinates.minLon = Math.min(coordinates.minLon, corner.getLon());
                        coordinates.maxLon = Math.max(coordinates.maxLon, corner.getLon());
                    }
                }
            }
            if (coordinates.maxLat < coordinates.minLat) {
                double temp = coordinates.maxLat;
                coordinates.maxLat = coordinates.minLat;
                coordinates.minLat = temp;
            }
            if (coordinates.maxLon < coordinates.minLon) {
                double temp = coordinates.maxLon;
                coordinates.maxLon = coordinates.minLon;
                coordinates.minLon = temp;
            }
            return coordinates;
        }

        private static GeoPos[] createCornerCoordinates(Product product) {
            PixelPos sceneUL = new PixelPos(0, 0);
            PixelPos sceneUR = new PixelPos(product.getSceneRasterWidth(), 0);
            PixelPos sceneLL = new PixelPos(0, product.getSceneRasterHeight());
            PixelPos sceneLR = new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight());
            GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
            GeoPos geoPosUL = sceneGeoCoding.getGeoPos(sceneUL, null);
            GeoPos geoPosUR = sceneGeoCoding.getGeoPos(sceneUR, null);
            GeoPos geoPosLL = sceneGeoCoding.getGeoPos(sceneLL, null);
            GeoPos geoPosLR = sceneGeoCoding.getGeoPos(sceneLR, null);
            return new GeoPos[]{geoPosUL, geoPosUR, geoPosLL, geoPosLR};
        }

    }

    static class TimeSpan {

        private Date startDate;
        private Date stopDate;

        private TimeSpan() {
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getStopDate() {
            return stopDate;
        }

        static TimeSpan create(List<Product> products) {
            TimeSpan timeSpan = new TimeSpan();
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
            utcCalendar.set(Calendar.MINUTE, 0);
            utcCalendar.set(Calendar.SECOND, 0);
            utcCalendar.set(Calendar.MILLISECOND, 0);
            timeSpan.startDate = utcCalendar.getTime();
            utcCalendar.set(1970, Calendar.JANUARY, 1);
            timeSpan.stopDate = utcCalendar.getTime();
            for (Product product : products) {
                ProductData.UTC startTime = product.getStartTime();
                if (startTime != null) {
                    timeSpan.startDate = min(timeSpan.startDate, startTime.getAsDate());
                }
                ProductData.UTC endTime = product.getEndTime();
                if (endTime != null) {
                    timeSpan.stopDate = max(timeSpan.stopDate, endTime.getAsDate());
                }
            }

            if (timeSpan.stopDate.before(timeSpan.startDate)) {
                Date temp = timeSpan.stopDate;
                timeSpan.stopDate = timeSpan.startDate;
                timeSpan.startDate = temp;
            }
            return timeSpan;
        }

        private static Date min(Date date1, Date date2) {
            return date1.before(date2) ? date1 : date2;
        }

        private static Date max(Date date1, Date date2) {
            return date1.after(date2) ? date1 : date2;
        }


    }
}