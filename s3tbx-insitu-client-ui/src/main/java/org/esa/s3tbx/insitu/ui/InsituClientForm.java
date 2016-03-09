package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.util.DateTimePicker;
import org.esa.snap.ui.DecimalFormatter;
import org.esa.snap.ui.UIUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Marco Peters
 */
class InsituClientForm extends JPanel {

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final DecimalFormatter DECIMAL_FORMATTER = new DecimalFormatter("###0.0##");

    private final InsituClientModel model;

    public InsituClientForm(InsituClientModel model) {
        this.model = model;
        initForm();
    }

    private void initForm() {

        final TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTablePadding(4, 4);
        layout.setTableWeightX(0.1);
        layout.setTableWeightY(0.0);

        setLayout(layout);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        layout.setCellWeightX(0, 1, 1.0);
        layout.setCellColspan(0, 1, 3);
        add(new JLabel("In-Situ Database:"));

        final JComboBox<InsituServerSpi> insituServerComboBox = new JComboBox<>(model.getInsituServerModel());
        insituServerComboBox.setPrototypeDisplayValue(InsituClientModel.NO_SELECTION_SERVER_SPI);
        insituServerComboBox.setRenderer(new InsituServerListCellRenderer());
        add(insituServerComboBox);

        layout.setCellWeightX(1, 1, 1.0);
        layout.setCellWeightX(1, 3, 1.0);
        layout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        layout.setCellAnchor(1, 0, TableLayout.Anchor.NORTHWEST);
        layout.setCellFill(1, 3, TableLayout.Fill.BOTH);
        layout.setCellAnchor(1, 2, TableLayout.Anchor.NORTHWEST);
        layout.setRowWeightY(1, 0.6);
        add(new JLabel("Dataset:"));
        final JList<InsituDataset> datasetList = new JList<>(model.getDatasetModel());
        datasetList.setSelectionModel(model.getDatasetSelectionModel());
        datasetList.setCellRenderer(new DatasetListCellRenderer());
        datasetList.setVisibleRowCount(6);
        add(new JScrollPane(datasetList));
        add(new JLabel("Parameter:"));
        final JList<InsituParameter> paramList = new JList<>(model.getParameterModel());
        paramList.setSelectionModel(model.getParameterSelectionModel());
        paramList.setCellRenderer(new ParameterListCellRenderer());
        paramList.setVisibleRowCount(6);
        add(new JScrollPane(paramList));

        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellColspan(2, 1, 3);
        layout.setCellFill(2, 1, TableLayout.Fill.BOTH);
        layout.setCellAnchor(2, 0, TableLayout.Anchor.NORTHWEST);
        layout.setCellWeightY(2, 1, 1.0);
        add(new JLabel("Product:"));
        final JList<Product> productList = new JList<>(model.getProductListModel());
        productList.setSelectionModel(model.getProductSelectionModel());
        productList.setCellRenderer(new ProductListCellRenderer());
        productList.setVisibleRowCount(6);
        add(new JScrollPane(productList));

        layout.setCellWeightX(3, 1, 1.0);
        layout.setCellWeightX(3, 3, 1.0);
        add(new JLabel("Start time:"));
        final DateTimePicker startDatePicker = new DateTimePicker(model.getStartDate(), Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        startDatePicker.addPropertyChangeListener("date", evt -> model.setStartDate((Date) evt.getNewValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_START_DATE, evt -> startDatePicker.setDateTime(model.getStartDate()));
        add(startDatePicker);

        add(new JLabel("Stop time:"));
        final DateTimePicker stopDatePicker = new DateTimePicker(model.getStopDate(), Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        stopDatePicker.addPropertyChangeListener("date", evt -> model.setStopDate((Date) evt.getNewValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_STOP_DATE, evt -> stopDatePicker.setDateTime(model.getStopDate()));
        add(stopDatePicker);

        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellWeightX(4, 3, 1.0);
        add(new JLabel("Min longitude:"));
        final JFormattedTextField minLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLonField.setValue(model.getMinLon());
        minLonField.addPropertyChangeListener("value", e -> model.setMinLon((double) minLonField.getValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_MIN_LON, new JFormattedTextFieldChangeListener(minLonField));
        add(minLonField);
        add(new JLabel("Max longitude:"));
        final JFormattedTextField maxLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLonField.setValue(model.getMaxLon());
        maxLonField.addPropertyChangeListener("value", e -> model.setMaxLon((double) maxLonField.getValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_MAX_LON, new JFormattedTextFieldChangeListener(maxLonField));
        add(maxLonField);

        layout.setCellWeightX(5, 1, 1.0);
        layout.setCellWeightX(5, 3, 1.0);
        add(new JLabel("Min latitude:"));
        final JFormattedTextField minLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLatField.setValue(model.getMinLat());
        minLatField.addPropertyChangeListener("value", e -> model.setMinLat((double) minLatField.getValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_MIN_LAT, new JFormattedTextFieldChangeListener(minLatField));
        add(minLatField);
        add(new JLabel("Max latitude:"));
        final JFormattedTextField maxLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLatField.setValue(model.getMaxLat());
        maxLatField.addPropertyChangeListener("value", e -> model.setMaxLat((double) maxLatField.getValue()));
        model.getChangeSupport().addPropertyChangeListener(InsituClientModel.PROPERTY_MAX_LAT, new JFormattedTextFieldChangeListener(maxLatField));
        add(maxLatField);

    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new WindowsLookAndFeel());
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            final ImageIcon imageIcon = UIUtils.loadImageIcon("/org/esa/s3tbx/insitu/insitu24.png", InsituClientForm.class);
            if (imageIcon != null) {
                frame.setIconImage(imageIcon.getImage());
            }
            frame.setTitle("In-Situ Data Access");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(new InsituClientForm(new InsituClientModel()));
            frame.pack();
            frame.setVisible(true);
        });
    }

    private static class InsituServerListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final InsituServerSpi server = (InsituServerSpi) value;
            label.setText(server.getName());
            label.setToolTipText(server.getDescription());
            return label;
        }
    }

    private static class DatasetListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final InsituDataset datasetDescr = (InsituDataset) value;
            label.setText(datasetDescr.getName());
            label.setToolTipText(datasetDescr.getDescription());
            final String text = String.format("<html>" +
                                              "<b>PI: </b>%s<br>" +
                                              "<b>Contact: </b>%s<br>" +
                                              "<b>Website: </b>%s<br>" +
                                              "<b>Description: </b>%s",
                                              datasetDescr.getPi(),
                                              datasetDescr.getContact(),
                                              datasetDescr.getWebsite(),
                                              datasetDescr.getDescription());
            label.setToolTipText(text);

            return label;
        }
    }

    private static class ParameterListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final InsituParameter insituParameter = (InsituParameter) value;
            label.setText(insituParameter.getName());
            label.setToolTipText(insituParameter.getDescription());
            final String text = String.format("<html>" +
                                              "<b>Type: </b>%s<br>" +
                                              "<b>Unit: </b>%s<br>" +
                                              "<b>Description: </b>%s",
                                              insituParameter.getType(),
                                              insituParameter.getUnit(),
                                              insituParameter.getDescription());

            label.setToolTipText(text);

            return label;
        }
    }

    private static class JFormattedTextFieldChangeListener implements PropertyChangeListener {

        private final JFormattedTextField textField;

        public JFormattedTextFieldChangeListener(JFormattedTextField textField) {
            this.textField = textField;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!textField.getValue().equals(evt.getNewValue())) {
                textField.setValue(evt.getNewValue());
            }
        }
    }

    private class ProductListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final Product product = (Product) value;
            label.setText(product.getDisplayName());
            label.setToolTipText(product.getDescription());

            return label;
        }

    }
}
