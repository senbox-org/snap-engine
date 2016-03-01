package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.insitu.server.InsituDatasetDescr;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.netbeans.api.progress.ProgressUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class InsituClientModel {


    static final InsituServerSpi NO_SELECTION_SERVER_SPI = new NoSelectionInsituServerSpi();

    private final DefaultComboBoxModel<InsituServerSpi> insituServerModel;
    private final DefaultListModel<InsituDatasetDescr> datasetModel;
    private final ListSelectionModel datasetSelectionModel;
    private final DefaultListModel<InsituParameter> parameterModel;
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

        productListModel = new DefaultListModel<>();
        productSelectionModel = new DefaultListSelectionModel();
        productSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productSelectionModel.addListSelectionListener(new ProductListSelectionListener());
        final ProductManager productManager = SnapApp.getDefault().getProductManager();
        productManagerListener = new PMListener(productManager);
        productManager.addListener(productManagerListener);

        Calendar utcCalendar = createUtcCalendar();
        startDate = utcCalendar.getTime();
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        stopDate = utcCalendar.getTime();
        minLon = -180.0;
        maxLon = 180.0;
        minLat = -90.0;
        maxLat = 90.0;

        changeSupport = new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

    public DefaultComboBoxModel<InsituServerSpi> getInsituServerModel() {
        return insituServerModel;
    }

    public DefaultListModel<InsituDatasetDescr> getDatasetModel() {
        return datasetModel;
    }

    public ListSelectionModel getDatasetSelectionModel() {
        return datasetSelectionModel;
    }

    public DefaultListModel<InsituParameter> getParameterModel() {
        return parameterModel;
    }

    public DefaultListModel<Product> getProductListModel() {
        return productListModel;
    }

    public DefaultListSelectionModel getProductSelectionModel() {
        return productSelectionModel;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
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
        changeSupport.firePropertyChange("minLon", minLon, oldValue);

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
        changeSupport.firePropertyChange("maxLon", maxLon, oldValue);
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
        changeSupport.firePropertyChange("minLat", minLat, oldValue);

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
        changeSupport.firePropertyChange("maxLat", maxLat, oldValue);
    }

    private static Calendar createUtcCalendar() {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
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
        public InsituServer createServer() throws Exception {
            return query -> InsituResponse.EMPTY_RESPONSE;
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
                final InsituServerSpi insituServerSpi = (InsituServerSpi) insituServerModel.getSelectedItem();
                try {
                    selectedServer = insituServerSpi.createServer();
                    ProgressHandleMonitor handle = ProgressHandleMonitor.create("Contacting " + insituServerSpi.getName() + " in-situ server");
                    ProgressUtils.runOffEventThreadWithProgressDialog(() -> updateModel(handle),
                                                                      "In-Situ Data Access",
                                                                      handle.getProgressHandle(),
                                                                      true,
                                                                      50,
                                                                      1000);

                } catch (Exception e) {
                    insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
                    throw new IllegalStateException("Could not create server instance for server '" + insituServerSpi.getName() + "'", e);
                }
            }

        }

        private void updateModel(ProgressMonitor pm) {
            pm.beginTask("Retrieving metadata from server", 2);
            try {
                InsituClientModel.this.updateDatasetModel();
                pm.worked(1);
                InsituClientModel.this.updateParameterModel();
                pm.worked(1);
            } catch (Exception e) {
                SnapApp.getDefault().handleError("Failed to retrieve metadata from server", e);
            } finally {
                pm.done();
            }
        }
    }

    private void updateDatasetModel() throws InsituServerException {
        getDatasetModel().clear();
        InsituQuery query = new InsituQuery().subject(InsituQuery.SUBJECT.DATASETS);
        final InsituResponse insituResponse = selectedServer.query(query);
        for (InsituDatasetDescr insituDataset : insituResponse.getDatasetDescriptions()) {
            datasetModel.addElement(insituDataset);
        }
    }

    private void updateParameterModel() throws InsituServerException {
        getParameterModel().clear();
        InsituQuery query = new InsituQuery().subject(InsituQuery.SUBJECT.PARAMETERS);
        if (!datasetSelectionModel.isSelectionEmpty()) {
            final int selectionIndex = datasetSelectionModel.getLeadSelectionIndex();
            final InsituDatasetDescr datasetDescr = datasetModel.get(selectionIndex);
            query.dataset(datasetDescr.getName());
        }
        final InsituResponse insituResponse = selectedServer.query(query);
        for (InsituParameter insituParameter : insituResponse.getParameters()) {
            parameterModel.addElement(insituParameter);
        }
    }

    private class DatasetListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent event) {
            try {
                updateParameterModel();
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
            final List<Product> products = getSelectedProducts();

            ProductData.UTC startDate = new ProductData.UTC(Double.MAX_VALUE);
            ProductData.UTC stopDate = new ProductData.UTC(Double.MIN_VALUE);
            double minLat = -90.0;
            double maxLat = 90.0;
            double minLon = -180.0;
            double maxLon = 180.0;
            for (Product product : products) {
                startDate = min(startDate, product.getStartTime());
                stopDate = max(stopDate, product.getEndTime());
                if(product.getSceneGeoCoding() != null) {
                    final GeoPos[] corners = createCornerCoordinates(product);
                    for (GeoPos corner : corners) {
                        minLat = Math.min(minLat, corner.getLat());
                        maxLat = Math.min(maxLat, corner.getLat());
                        minLon = Math.min(minLon, corner.getLon());
                        maxLon = Math.min(maxLon, corner.getLon());
                    }
                }
            }
            setMinLat(minLat);
            setMaxLat(maxLat);
            setMinLon(minLon);
            setMaxLon(maxLon);
        }

        private GeoPos[] createCornerCoordinates(Product product) {
            PixelPos sceneUL = new  PixelPos(0 + 0.5f, 0 + 0.5f);
            PixelPos sceneUR = new  PixelPos(product.getSceneRasterWidth() - 1 + 0.5f, 0 + 0.5f);
            PixelPos sceneLL = new  PixelPos(0 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            PixelPos sceneLR = new  PixelPos(product.getSceneRasterWidth() - 1 + 0.5f, product.getSceneRasterHeight() - 1 + 0.5f);
            GeoPos geoPosUL = product.getSceneGeoCoding().getGeoPos(sceneUL, null);
            GeoPos geoPosUR = product.getSceneGeoCoding().getGeoPos(sceneUR, null);
            GeoPos geoPosLL = product.getSceneGeoCoding().getGeoPos(sceneLL, null);
            GeoPos geoPosLR = product.getSceneGeoCoding().getGeoPos(sceneLR, null);
            return new GeoPos[]{geoPosUL, geoPosUR, geoPosLL, geoPosLR};
        }


        private ProductData.UTC min(ProductData.UTC startDate1, ProductData.UTC startDate2) {
            return startDate1.getAsDate().before(startDate2.getAsDate()) ? startDate1 : startDate2;
        }

        private ProductData.UTC max(ProductData.UTC startDate1, ProductData.UTC startDate2) {
            return startDate1.getAsDate().after(startDate2.getAsDate()) ? startDate1 : startDate2;
        }

        private List<Product> getSelectedProducts() {
            int iMin = productSelectionModel.getMinSelectionIndex();
            int iMax = productSelectionModel.getMaxSelectionIndex();

            final ArrayList<Product> productList = new ArrayList<>();
//            if (iMin < 0 || iMax < 0) {
//                return productList;
//            }

            for (int i = iMin; i < iMax; i++) {
                if (productSelectionModel.isSelectedIndex(i)) {
                    productList.add(productList.get(i));
                }

            }

            return productList;
        }
    }
}