package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.rcp.util.DateTimePicker;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Marco Peters
 */
public class InsituClientForm extends JPanel {

    public InsituClientForm() {
        initForm();
    }

    private void initForm() {
        setLayout(new VerticalLayout(4));

        add(createDatabaseSelectionPanel());
        add(createRequestConfigurationPanel());
    }

    private JPanel createRequestConfigurationPanel() {
        final JPanel panel = new JPanel(new HorizontalLayout(2));
        panel.setLayout(new HorizontalLayout(2));
        panel.add(new JLabel("In-Situ Database:"));
        panel.add(new JComboBox<>(new String[]{"Mermaid"}));
        return panel;
    }

    private JPanel createDatabaseSelectionPanel() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 12);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);

        final TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);
        layout.setTableWeightX(1.0);

        layout.setCellWeightX(0, 0, 0.0);
        layout.setCellColspan(0, 1, 3);
        layout.setCellWeightX(0, 1, 2.0);
        layout.setCellWeightX(1, 1, 1.3);
        layout.setCellWeightX(1, 3, 1.3);
        layout.setCellColspan(2, 1, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("In-Situ Database:"));
        panel.add(new JComboBox<>(new String[]{"MERMAID"}));
        panel.add(new JLabel("Campaign:"));
        final JList<String> campaignList = new JList<>(new String[]{"BUSSOLE", "AERONET"});
        campaignList.setVisibleRowCount(6);
        panel.add(new JScrollPane(campaignList));
        panel.add(new JLabel("Parameter:"));
        final JList<String> paramList = new JList<>(new String[]{
                "chlorophyll_a_total", "es_412", "es_443", "es_490", "es_510", "es_560", "es_665",
                "es_683", "kd_412", "kd_443", "kd_490", "kd_510", "kd_560", "kd_665", "kd_683", "ozone_concentration"
        });
        paramList.setVisibleRowCount(6);
        panel.add(new JScrollPane(paramList));
        panel.add(new JLabel("Product:"));
        panel.add(new JComboBox<>(new String[]{"S3A_OL_2_WFR____20100602T094537_20100602T094837_2015070..."}));
        panel.add(new JLabel("Start time:"));
        utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
        panel.add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(),
                                     new SimpleDateFormat("dd-MMM-yyyy"), new SimpleDateFormat("HH:mm:ss")));
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        panel.add(new JLabel("End time:"));
        panel.add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(), new SimpleDateFormat("dd-MMM-yyyy"),
                                     new SimpleDateFormat("HH:mm:ss")));


        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setTitle("In-Siut Data Access");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(new InsituClientForm());
            frame.pack();
            frame.setVisible(true);
        });
    }

}
