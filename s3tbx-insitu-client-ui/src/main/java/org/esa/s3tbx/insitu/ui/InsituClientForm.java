package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.rcp.util.DateTimePicker;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
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
        layout.setRowWeightY(1, 1.0);
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
        add(new JLabel("Product:"));
        add(new JComboBox<>(new String[]{"S3A_OL_2_WFR____20100602T094537_20100602T094837_2015070..."}));

        add(new JLabel("Start time:"));
        utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
        add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(),
                                     new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss"), new SimpleDateFormat("HH:mm:ss")));
        utcCalendar.add(Calendar.DAY_OF_YEAR, 2);
        add(new JLabel("End time:"));
        add(new DateTimePicker(utcCalendar.getTime(), Locale.getDefault(), new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss"),
                                     new SimpleDateFormat("HH:mm:ss")));

        add(new JLabel("Longitude Minimum:"));
        add(new JFormattedTextField());

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Locale.setDefault(Locale.ENGLISH);
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            JFrame frame = new JFrame();
            frame.setTitle("In-Siut Data Access");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(new InsituClientForm());
            frame.pack();
            frame.setVisible(true);
        });
    }

}
