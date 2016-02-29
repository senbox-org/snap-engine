package org.esa.s3tbx.insitu.ui;

import org.esa.s3tbx.insitu.server.InsituDatasetDescr;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.Product;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

public class InsituClientModel implements Serializable {


    static final InsituServerSpi NO_SELECTION_SERVER_SPI = new NoSelectionInsituServerSpi();

    private final DefaultComboBoxModel<InsituServerSpi> insituServerModel;
    private final DefaultListModel<InsituDatasetDescr> datasetModel;
    private final DefaultListModel<InsituParameter> parameterModel;
    private final DefaultListModel<Product> productListModel;
    private Date startDate;
    private Date stopDate;
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;

    private InsituServer selectedServer;

    public InsituClientModel() {
        final Set<InsituServerSpi> allRegisteredServers = InsituServerRegistry.getInstance().getAllRegisteredServers();
        InsituServerSpi[] servers = allRegisteredServers.toArray(new InsituServerSpi[0]);
        insituServerModel = new DefaultComboBoxModel<>(servers);
        insituServerModel.insertElementAt(NO_SELECTION_SERVER_SPI, 0);
        insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
        insituServerModel.addListDataListener(new ServerListener());
        datasetModel = new DefaultListModel<>();
        parameterModel = new DefaultListModel<>();
        productListModel = new DefaultListModel<>();
        Calendar utcCalendar = createUtcCalendar();
        startDate = utcCalendar.getTime();
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        stopDate = utcCalendar.getTime();
        minLon = -180.0;
        maxLon = 180.0;
        minLat = -90.0;
        maxLat = 90.0;
    }

    public DefaultComboBoxModel<InsituServerSpi> getInsituServerModel() {
        return insituServerModel;
    }

    public DefaultListModel<InsituDatasetDescr> getDatasetModel() {
        return datasetModel;
    }

    public DefaultListModel<InsituParameter> getParameterModel() {
        return parameterModel;
    }

    public DefaultListModel<Product> getProductListModel() {
        return productListModel;
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
        this.minLon = minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
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
            return null;
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
            if(event.getIndex0() == -1 && event.getIndex1() == -1) {
                // selection changed if both indices are -1
                final InsituServerSpi insituServerSpi = (InsituServerSpi) insituServerModel.getSelectedItem();
                try {
                    selectedServer = insituServerSpi.createServer();
                    // todo (mp/29.02.2016) - use Progress Monitor
                    updateDatasetModel();
                    updateParameterModel();
                } catch (Exception e) {
                    insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
                    throw new IllegalStateException("Could not create server instance for server '" + insituServerSpi.getName() + "'", e);
                }
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
        final InsituResponse insituResponse = selectedServer.query(query);
        for (InsituParameter insituParameter : insituResponse.getParameters()) {
            parameterModel.addElement(insituParameter);
        }
    }
}