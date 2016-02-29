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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Marco Peters
 */
public class InsituClientForm extends JPanel {

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final DecimalFormatter DECIMAL_FORMATTER = new DecimalFormatter("###0.0##");

    private final InsituClientModel model;

    public InsituClientForm() {

        model = new InsituClientModel();
        initForm();
    }

    private void initForm() {

        final TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
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
        insituServerComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final ItemSelectable itemSelectable = e.getItemSelectable();
                System.out.println("itemSelectable = " + itemSelectable);
                final Object item = e.getItem();
                System.out.println("item = " + item);
                if(e.getID() == ItemEvent.DESELECTED) {
                    System.out.println("eventId = DESELECTED");
                }else if(e.getID() == ItemEvent.SELECTED){
                    System.out.println("eventId = SELECTED");
                }else if(e.getID() == ItemEvent.ITEM_STATE_CHANGED){
                    System.out.println("eventId = ITEM_STATE_CHANGED");
                }
                // todo
                // - get datasets from server
                // - get parameters from server
                // todo

            }
        });
        add(insituServerComboBox);

        layout.setCellWeightX(1, 1, 1.0);
        layout.setCellWeightX(1, 3, 1.0);
        layout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        layout.setCellFill(1, 3, TableLayout.Fill.BOTH);
        layout.setRowWeightY(1, 0.6);
        add(new JLabel("Dataset:"));
        final JList<InsituDataset> campaignList = new JList<>(model.getDatasetModel());
        campaignList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        campaignList.setVisibleRowCount(6);
        add(new JScrollPane(campaignList));
        add(new JLabel("Parameter:"));
        final JList<InsituParameter> paramList = new JList<>(model.getParameterModel());
        paramList.setVisibleRowCount(6);
        add(new JScrollPane(paramList));

        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellColspan(2, 1, 3);
        layout.setCellFill(2, 2, TableLayout.Fill.BOTH);
        layout.setCellWeightY(2, 2, 1.0);
        add(new JLabel("Product:"));
        final JList<Product> productList = new JList<>(model.getProductListModel());
        productList.setVisibleRowCount(6);
        add(new JScrollPane(productList));

        layout.setCellWeightX(3, 1, 1.0);
        layout.setCellWeightX(3, 3, 1.0);
        add(new JLabel("Start time:"));
        final DateTimePicker startDatePicker = new DateTimePicker(model.getStartDate(), Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        startDatePicker.addPropertyChangeListener("date", evt -> model.setStartDate((Date) evt.getNewValue()));
        add(startDatePicker);

        add(new JLabel("Stop time:"));
        final DateTimePicker stopDatePicker = new DateTimePicker(model.getStopDate(), Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        stopDatePicker.addPropertyChangeListener("date", evt -> model.setStopDate((Date) evt.getNewValue()));
        add(stopDatePicker);

        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellWeightX(4, 3, 1.0);
        add(new JLabel("Min longitude:"));
        final JFormattedTextField minLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLonField.addActionListener(e -> model.setMinLon((double) minLonField.getValue()));
        add(minLonField);
        add(new JLabel("Max longitude:"));
        final JFormattedTextField maxLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLonField.addActionListener(e -> model.setMaxLon((double) maxLonField.getValue()));
        add(maxLonField);

        layout.setCellWeightX(5, 1, 1.0);
        layout.setCellWeightX(5, 3, 1.0);
        add(new JLabel("Min latitude:"));
        final JFormattedTextField minLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLatField.addActionListener(e -> model.setMinLat((double) minLatField.getValue()));
        add(minLatField);
        add(new JLabel("Max latitude:"));
        final JFormattedTextField maxLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLatField.addActionListener(e -> model.setMaxLat((double) maxLatField.getValue()));
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
            frame.getContentPane().add(new InsituClientForm());
            frame.pack();
            frame.setVisible(true);
        });
    }

    private static class InsituServerListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final InsituServerSpi server = (InsituServerSpi)value;
            label.setText(server.getName());
            label.setToolTipText(server.getDescription());
            return label;
        }
    }

}
