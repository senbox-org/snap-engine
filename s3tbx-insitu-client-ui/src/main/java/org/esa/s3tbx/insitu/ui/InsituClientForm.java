package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.util.DateTimePicker;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.HelpCtx;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Marco Peters
 */
public class InsituClientForm extends JPanel {

    public InsituClientForm(HelpCtx helpCtx) {
        initForm(helpCtx);
    }

    private void initForm(HelpCtx helpCtx) {
        final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);

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
        add(new JComboBox<>(new String[]{"MERMAID"}));

        layout.setCellWeightX(1, 1, 1.0);
        layout.setCellWeightX(1, 3, 1.0);
        layout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        layout.setCellFill(1, 3, TableLayout.Fill.BOTH);
        layout.setRowWeightY(1, 0.6);
        add(new JLabel("Campaign:"));
        final JList<String> campaignList = new JList<>(new String[]{"BUSSOLE", "AERONET"});
        campaignList.setVisibleRowCount(6);
        add(new JScrollPane(campaignList));
        add(new JLabel("Parameter:"));
        final JList<String> paramList = new JList<>(new String[]{
                "chlorophyll_a_total", "es_412", "es_443", "es_490", "es_510", "es_560", "es_665",
                "es_683", "kd_412", "kd_443", "kd_490", "kd_510", "kd_560", "kd_665", "kd_683", "ozone_concentration"
        });
        paramList.setVisibleRowCount(6);
        add(new JScrollPane(paramList));

        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellColspan(2, 1, 3);
        layout.setCellFill(2, 2, TableLayout.Fill.BOTH);
        layout.setCellWeightY(2, 2, 1.0);
        add(new JLabel("Product:"));
        final JList<String> productList = new JList<>(new String[]{"S3A_OL_2_WFR____20100602T094537_20100602T094837_2015070..."});
        productList.setVisibleRowCount(6);
        add(new JScrollPane(productList));


        layout.setCellWeightX(3, 1, 1.0);
        layout.setCellWeightX(3, 3, 1.0);
        add(new JLabel("Start time:"));
        utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
        final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(), defaultDateFormat, timeFormat));
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        add(new JLabel("End time:"));
        add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(), defaultDateFormat, timeFormat));

        layout.setCellWeightX(4, 1, 1.0);
        layout.setCellWeightX(4, 3, 1.0);
        add(new JLabel("Min longitude:"));
        add(new JTextField());
        add(new JLabel("Max longitude:"));
        add(new JTextField());

        layout.setCellWeightX(5, 1, 1.0);
        layout.setCellWeightX(5, 3, 1.0);
        add(new JLabel("Min latitude:"));
        add(new JTextField());
        add(new JLabel("Max latitude:"));
        add(new JTextField());

//        layout.setCellColspan(6, 0, 4);
//        layout.setRowWeightX(6, 1.0);
//        layout.setRowWeightY(6, 1.0);
//        layout.setRowFill(6, TableLayout.Fill.BOTH);
//        add(createStatusPanel(helpCtx));
        // maybe later we will add a preview table of the data
//        add(createPreviewTablePanel(helpCtx));

    }

    private Component createStatusPanel(HelpCtx helpCtx) {
        final TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(0.5);
        layout.setCellWeightX(0, 2, 2.0);
        layout.setCellWeightX(0, 3, 0.0);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);

        final JPanel contentPanel = new JPanel(layout);
        final AbstractButton refreshButton = ToolButtonFactory.createButton(TangoIcons.actions_view_refresh(TangoIcons.Res.R22), false);
        refreshButton.setText("#Obs: 486");
        refreshButton.setName("refreshButton");
        contentPanel.add(refreshButton);
        final AbstractButton downloadButton = ToolButtonFactory.createButton(TangoIcons.actions_document_save(TangoIcons.Res.R22), false);
        downloadButton.setText("Download");
        downloadButton.setName("downloadButton");
        contentPanel.add(downloadButton);
        contentPanel.add(layout.createHorizontalSpacer());

        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(helpCtx), false);
        helpButton.setName("helpButton");
        contentPanel.add(helpButton);

        return contentPanel;
    }

    private Component createPreviewTablePanel(HelpCtx helpCtx) {
        final JPanel contentPanel = new JPanel(new BorderLayout(4, 4));

        final JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.setBorder(new TitledBorder("Observations"));
        final JTable jTable = new JTable(15, 6);
        jTable.setFillsViewportHeight(false);
        tablePanel.add(new JScrollPane(jTable), BorderLayout.CENTER);
        final TableLayout navigationLayout = new TableLayout(8);
        navigationLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        navigationLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        navigationLayout.setTablePadding(4, 4);
        final JPanel navigationPanel = new JPanel(navigationLayout);
        navigationPanel.add(new JLabel("#Observations:"));
        final JLabel numObsLabel = new JLabel("468");
        navigationPanel.add(numObsLabel);
        navigationPanel.add(navigationLayout.createHorizontalSpacer());
        final AbstractButton goFirstButton = ToolButtonFactory.createButton(TangoIcons.actions_go_first(TangoIcons.Res.R16), false);
        goFirstButton.setName("goFirstButton");
        navigationPanel.add(goFirstButton);
        final AbstractButton goPreviousButton = ToolButtonFactory.createButton(TangoIcons.actions_go_previous(TangoIcons.Res.R16), false);
        goFirstButton.setName("goPreviousButton");
        navigationPanel.add(goPreviousButton);
        navigationPanel.add(new JLabel("3 / 6"));
        final AbstractButton goNextButton = ToolButtonFactory.createButton(TangoIcons.actions_go_next(TangoIcons.Res.R16), false);
        goFirstButton.setName("goNextButton");
        navigationPanel.add(goNextButton);
        final AbstractButton goLastButton = ToolButtonFactory.createButton(TangoIcons.actions_go_last(TangoIcons.Res.R16), false);
        goLastButton.setName("goLastButton");
        navigationPanel.add(goLastButton);
        tablePanel.add(navigationPanel, BorderLayout.SOUTH);
        contentPanel.add(tablePanel, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel();
        final TableLayout buttonLayout = new TableLayout(1);
        buttonLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        buttonLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        buttonLayout.setTablePadding(4, 4);
        buttonLayout.setTableWeightX(1.0);
        buttonPanel.setLayout(buttonLayout);
        final AbstractButton refreshButton = ToolButtonFactory.createButton(TangoIcons.actions_view_refresh(TangoIcons.Res.R22), false);
        refreshButton.setName("refreshButton");
        buttonPanel.add(refreshButton);
        final AbstractButton downloadButton = ToolButtonFactory.createButton(TangoIcons.actions_document_save(TangoIcons.Res.R22), false);
        downloadButton.setName("downloadButton");
        buttonPanel.add(downloadButton);
        buttonPanel.add(buttonLayout.createVerticalSpacer());
        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(helpCtx), false);
        helpButton.setName("helpButton");
        buttonPanel.add(helpButton);
        contentPanel.add(buttonPanel, BorderLayout.EAST);

        return contentPanel;
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
            frame.getContentPane().add(new InsituClientForm(new HelpCtx("insituClientTool")));
            frame.pack();
            frame.setVisible(true);
        });
    }

}
