package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.combobox.FileChooserComboBox;
import com.jidesoft.utils.Lm;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.GridBagUtils;

import javax.media.jai.JAI;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.text.NumberFormatter;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.System;
import java.text.DecimalFormat;
import java.text.ParseException;

public class SpeedTester {

    private JTextArea textArea;

    public SpeedTester() {
        org.esa.beam.util.SystemUtils.init3rdPartyLibs(SpeedTester.class.getClassLoader());
        createUI();
    }

    private void createUI() {
        final JFrame frame = new JFrame("Speed test");
        frame.setSize(new Dimension(650, 450));

        final JPanel panel = GridBagUtils.createPanel();

        final JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(36.0, 0.1, Double.POSITIVE_INFINITY, 0.1));

        final JSpinner tileCacheCapacitySpinner = new JSpinner(new SpinnerNumberModel(512, 1, Integer.MAX_VALUE, 1));

        textArea = new JTextArea();

        final JButton runButton = new JButton("Run speed test");

        final FileChooserComboBox fileChooserComboBox = new FileChooserComboBox();
        fileChooserComboBox.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selectedItem")) {
                    runButton.setEnabled(true);
                }

            }
        });

        runButton.setEnabled(false);
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final double tileCacheCapacityasDouble = Double.parseDouble(tileCacheCapacitySpinner.getValue().toString());
                    JAI.getDefaultInstance().getTileCache().setMemoryCapacity((long) tileCacheCapacityasDouble * 1000000);
                    final String fileName = fileChooserComboBox.getSelectedItem().toString();
                    final double highestPossibleSpeed = Double.parseDouble(speedSpinner.getValue().toString());
                    runSpeedTest(fileName, highestPossibleSpeed);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        GridBagConstraints gbc = GridBagUtils.createConstraints("insets=4, anchor=NORTHWEST, fill=HORIZONTAL, weightx=1");
        GridBagUtils.addToPanel(panel, new JLabel("Highest Possible Reading Speed in MB/s:"), gbc, "gridx=0, gridy=0");
        GridBagUtils.addToPanel(panel, speedSpinner, gbc, "gridx=1, gridy=0");
        GridBagUtils.addToPanel(panel, new JLabel("Tile Cache Capacity in MB:"), gbc, "gridx=0, gridy=2");
        GridBagUtils.addToPanel(panel, tileCacheCapacitySpinner, gbc, "gridx=1, gridy=2");
        GridBagUtils.addToPanel(panel, fileChooserComboBox, gbc, "gridx=0, gridy=3, gridwidth=2");
        JScrollPane textAreaScrollPane = new JScrollPane(textArea);
        GridBagUtils.addToPanel(panel, textAreaScrollPane, gbc, "gridx=0, gridy=4, fill=BOTH, weighty=1");
        GridBagUtils.addToPanel(panel, runButton, gbc, "gridx=0, gridy=5, anchor=EAST, fill=NONE, weighty=0");

        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    private void runSpeedTest(String fileName, final double highestPossibleSpeed) throws IOException {
        final Product product = ProductIO.readProduct(fileName);
        textArea.setText("Info for " + product.getDisplayName() + ":\n");
        final Band[] productBands = product.getBands();
        for (final Band productBand : productBands) {
            SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    final String bandInfo = getBandInfo(highestPossibleSpeed, productBand);
                    textArea.append(bandInfo);
                    return null;
                }
            };
            worker.execute();
        }
    }

    private synchronized String getBandInfo(double highestPossibleSpeed, Band productBand) throws ParseException {
        final double rawStorageSizeInMB = (double) productBand.getRawStorageSize() / (1024 * 1024);
        final Product product = productBand.getProduct();
        final int width = product.getSceneRasterWidth();
        final ProductData destBuffer = productBand.createCompatibleRasterData(width, 1);
        final ProductReader productReader = product.getProductReader();
        final long before = System.nanoTime();
        for(int i = 0; i < product.getSceneRasterHeight(); i++) {
            try {
                productReader.readBandRasterData(productBand, 0, i, width, 1, destBuffer, ProgressMonitor.NULL);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final long after = System.nanoTime();
        return constructInfoString(highestPossibleSpeed, productBand.getName(), rawStorageSizeInMB, before, after);
    }

    private String constructInfoString(double highestPossibleSpeed, String name, double rawStorageSizeInMB, long before, double after) throws ParseException {
        final double elapsedTimeInSeconds = (after - before) / 1.0e9;
        final double speed = rawStorageSizeInMB / elapsedTimeInSeconds;
        final double percentage = (speed / highestPossibleSpeed) * 100.0;
        NumberFormatter formatter = new NumberFormatter(new DecimalFormat());
        String formattedSpeed = formatter.valueToString(speed);
        String formattedPercentage = formatter.valueToString(percentage);
        String formattedTimeInSeconds = formatter.valueToString(elapsedTimeInSeconds);
        return "Read '" + name + "' \t at " + formattedSpeed + " MB/s  \t (" + formattedPercentage + "%) \t Elapsed time: "
                + formattedTimeInSeconds + "s \n";
    }

    public static void main(String[] args) throws IOException {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
        new SpeedTester();
    }

}