package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.util.DateTimePicker;
import org.esa.snap.ui.DecimalFormatter;
import org.esa.snap.ui.UIUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author Marco Peters
 */
public class InsituClientForm extends JPanel {

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final DecimalFormatter DECIMAL_FORMATTER = new DecimalFormatter("###0.0##");

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

    public InsituClientForm() {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);

        final Set<InsituServerSpi> allRegisteredServers = InsituServerRegistry.getInstance().getAllRegisteredServers();
        InsituServerSpi[] servers = allRegisteredServers.toArray(new InsituServerSpi[0]);
        insituServerModel = new DefaultComboBoxModel<>(servers);
        insituServerModel.insertElementAt(NO_SELECTION, 0);
        datasetModel = new DefaultListModel<>();
        parameterModel = new DefaultListModel<>();
        productListModel = new DefaultListModel<>();
        utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
        startDate = utcCalendar.getTime();
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        stopDate = utcCalendar.getTime();

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

        final JComboBox<InsituServerSpi> insituServerComboBox = new JComboBox<>(insituServerModel);
        insituServerComboBox.setPrototypeDisplayValue(new InsituPrototype());
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
        final JList<InsituDataset> campaignList = new JList<>(datasetModel);
        campaignList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        campaignList.setVisibleRowCount(6);
        add(new JScrollPane(campaignList));
        add(new JLabel("Parameter:"));
        final JList<InsituParameter> paramList = new JList<>(parameterModel);
        paramList.setVisibleRowCount(6);
        add(new JScrollPane(paramList));

        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellColspan(2, 1, 3);
        layout.setCellFill(2, 2, TableLayout.Fill.BOTH);
        layout.setCellWeightY(2, 2, 1.0);
        add(new JLabel("Product:"));
        final JList<Product> productList = new JList<>(productListModel);
        productList.setVisibleRowCount(6);
        add(new JScrollPane(productList));

        layout.setCellWeightX(3, 1, 1.0);
        layout.setCellWeightX(3, 3, 1.0);
        add(new JLabel("Start time:"));
        final DateTimePicker startDatePicker = new DateTimePicker(startDate, Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        startDatePicker.addPropertyChangeListener("date", evt -> startDate = (Date)evt.getNewValue());
        add(startDatePicker);

        add(new JLabel("Stop time:"));
        final DateTimePicker stopDatePicker = new DateTimePicker(stopDate, Locale.getDefault(), DEFAULT_DATE_FORMAT, TIME_FORMAT);
        stopDatePicker.addPropertyChangeListener("date", evt -> stopDate = (Date)evt.getNewValue());
        add(stopDatePicker);

        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellWeightX(4, 3, 1.0);
        add(new JLabel("Min longitude:"));
        final JFormattedTextField minLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLonField.addActionListener(e -> minLon = (double) minLonField.getValue());
        add(minLonField);
        add(new JLabel("Max longitude:"));
        final JFormattedTextField maxLonField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLonField.addActionListener(e -> maxLon = (double) maxLonField.getValue());
        add(maxLonField);

        layout.setCellWeightX(5, 1, 1.0);
        layout.setCellWeightX(5, 3, 1.0);
        add(new JLabel("Min latitude:"));
        final JFormattedTextField minLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        minLatField.addActionListener(e -> minLat = (double) minLatField.getValue());
        add(minLatField);
        add(new JLabel("Max latitude:"));
        final JFormattedTextField maxLatField = new JFormattedTextField(DECIMAL_FORMATTER);
        maxLatField.addActionListener(e -> maxLat = (double) maxLatField.getValue());
        add(maxLatField);

//        layout.setCellColspan(6, 0, 4);
//        layout.setRowWeightX(6, 1.0);
//        layout.setRowWeightY(6, 1.0);
//        layout.setRowFill(6, TableLayout.Fill.BOTH);
        // maybe later we will add a preview table of the data
//        add(createPreviewTablePanel(helpCtx));

    }

//    private Component createPreviewTablePanel(HelpCtx helpCtx) {
//        final JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
//
//        final JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
//        tablePanel.setBorder(new TitledBorder("Observations"));
//        final JTable jTable = new JTable(15, 6);
//        jTable.setFillsViewportHeight(false);
//        tablePanel.add(new JScrollPane(jTable), BorderLayout.CENTER);
//        final TableLayout navigationLayout = new TableLayout(8);
//        navigationLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
//        navigationLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
//        navigationLayout.setTablePadding(4, 4);
//        final JPanel navigationPanel = new JPanel(navigationLayout);
//        navigationPanel.add(new JLabel("#Observations:"));
//        final JLabel numObsLabel = new JLabel("468");
//        navigationPanel.add(numObsLabel);
//        navigationPanel.add(navigationLayout.createHorizontalSpacer());
//        final AbstractButton goFirstButton = ToolButtonFactory.createButton(TangoIcons.actions_go_first(TangoIcons.Res.R16), false);
//        goFirstButton.setName("goFirstButton");
//        navigationPanel.add(goFirstButton);
//        final AbstractButton goPreviousButton = ToolButtonFactory.createButton(TangoIcons.actions_go_previous(TangoIcons.Res.R16), false);
//        goFirstButton.setName("goPreviousButton");
//        navigationPanel.add(goPreviousButton);
//        navigationPanel.add(new JLabel("3 / 6"));
//        final AbstractButton goNextButton = ToolButtonFactory.createButton(TangoIcons.actions_go_next(TangoIcons.Res.R16), false);
//        goFirstButton.setName("goNextButton");
//        navigationPanel.add(goNextButton);
//        final AbstractButton goLastButton = ToolButtonFactory.createButton(TangoIcons.actions_go_last(TangoIcons.Res.R16), false);
//        goLastButton.setName("goLastButton");
//        navigationPanel.add(goLastButton);
//        tablePanel.add(navigationPanel, BorderLayout.SOUTH);
//        contentPanel.add(tablePanel, BorderLayout.CENTER);
//
//        final JPanel buttonPanel = new JPanel();
//        final TableLayout buttonLayout = new TableLayout(1);
//        buttonLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
//        buttonLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
//        buttonLayout.setTablePadding(4, 4);
//        buttonLayout.setTableWeightX(1.0);
//        buttonPanel.setLayout(buttonLayout);
//        final AbstractButton refreshButton = ToolButtonFactory.createButton(TangoIcons.actions_view_refresh(TangoIcons.Res.R22), false);
//        refreshButton.setName("refreshButton");
//        buttonPanel.add(refreshButton);
//        final AbstractButton downloadButton = ToolButtonFactory.createButton(TangoIcons.actions_document_save(TangoIcons.Res.R22), false);
//        downloadButton.setName("downloadButton");
//        buttonPanel.add(downloadButton);
//        buttonPanel.add(buttonLayout.createVerticalSpacer());
//        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(helpCtx), false);
//        helpButton.setName("helpButton");
//        buttonPanel.add(helpButton);
//        contentPanel.add(buttonPanel, BorderLayout.EAST);
//
//        return contentPanel;
//    }

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

    private static class InsituPrototype implements InsituServerSpi {

        @Override
        public String getName() {
            return "THIS_IS_A_PROTOTYPE_NAME";
        }

        @Override
        public String getDescription() {
            return "SOME_PROTOTYPE_DESCRIPTION";
        }

        @Override
        public InsituServer createServer() throws Exception {
            return null;
        }
    }

    private static final InsituServerSpi NO_SELECTION = new InsituServerSpi() {
        @Override
        public String getName() {
            return "<NO_SERVER_SELECTED>";
        }

        @Override
        public String getDescription() {
            return "SOME_PROTOTYPE_DESCRIPTION";
        }

        @Override
        public InsituServer createServer() throws Exception {
            return null;
        }
    };
}
