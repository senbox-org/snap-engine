package org.esa.s3tbx.insitu.ui;

import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.Product;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

public class InsituClientModel implements Serializable {


    static final InsituServerSpi NO_SELECTION_SERVER_SPI = new NoSelectionInsituServerSpi();

    private DefaultComboBoxModel<InsituServerSpi> insituServerModel;
    private DefaultListModel<InsituDataset> datasetModel;
    private DefaultListModel<InsituParameter> parameterModel;
    private DefaultListModel<Product> productListModel;
    private Date startDate;
    private Date stopDate;
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;

    public InsituClientModel() {
        final Set<InsituServerSpi> allRegisteredServers = InsituServerRegistry.getInstance().getAllRegisteredServers();
        InsituServerSpi[] servers = allRegisteredServers.toArray(new InsituServerSpi[0]);
        insituServerModel = new DefaultComboBoxModel<>(servers);
        insituServerModel.insertElementAt(NO_SELECTION_SERVER_SPI, 0);
        insituServerModel.setSelectedItem(NO_SELECTION_SERVER_SPI);
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

    public DefaultListModel<InsituDataset> getDatasetModel() {
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
}