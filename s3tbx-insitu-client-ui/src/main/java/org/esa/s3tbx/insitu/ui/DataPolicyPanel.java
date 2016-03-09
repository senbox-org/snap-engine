package org.esa.s3tbx.insitu.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.UriLabel;
import com.jidesoft.swing.MultilineLabel;
import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

/**
 * @author Marco Peters
 */
class DataPolicyPanel extends JPanel {

    private ButtonModel acceptModel;

    public DataPolicyPanel(InsituDataset insituDataset) {
        TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTablePadding(4, 4);
        layout.setRowWeightX(0, 0.0);
        setLayout(layout);

        String datasetLabelText = "<html><b>" + insituDataset.getName() + "</b>";
        if (insituDataset.getWebsite() != null && !insituDataset.getWebsite().isEmpty()) {
            URI websiteUri = null;
            try {
                websiteUri = new URI(insituDataset.getWebsite());
            } catch (URISyntaxException e) {
                SystemUtils.LOG.log(Level.SEVERE, "Could not create website url.", e);
            }
            JLabel datasetNameLabel = new UriLabel(datasetLabelText, websiteUri);
            datasetNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            add(datasetNameLabel);
        }else {
            add(new JLabel(datasetLabelText));
        }
        URI mailtoUri = null;
        try {
            mailtoUri = new URI("mailto:" + insituDataset.getContact());
        } catch (URISyntaxException e) {
            SystemUtils.LOG.log(Level.SEVERE, "Could not create mailto-url.", e);
        }
        JLabel contact = new UriLabel(insituDataset.getPi(), mailtoUri);
        contact.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(contact);
        add(layout.createHorizontalSpacer());

        JCheckBox accept = new JCheckBox("Accept");
        acceptModel = accept.getModel();
        add(accept);

        layout.setRowWeightY(1, 1.0);
        layout.setCellColspan(1, 0, 4);
        layout.setRowFill(1, TableLayout.Fill.BOTH);
        MultilineLabel policyTextArea = new MultilineLabel();
        policyTextArea.setColumns(80);
        policyTextArea.setRows(6);
        policyTextArea.setBorder(new LineBorder(Color.BLACK));
        policyTextArea.setText(insituDataset.getPolicy());
        add(policyTextArea);
    }

    public boolean isAccepted() {
        return acceptModel.isSelected();
    }

    public void setAccepted(boolean accept) {
        acceptModel.setSelected(accept);
    }
}
