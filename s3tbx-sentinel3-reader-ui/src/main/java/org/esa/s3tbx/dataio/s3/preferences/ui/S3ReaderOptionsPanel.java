/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.dataio.s3.preferences.ui;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class S3ReaderOptionsPanel extends javax.swing.JPanel {

    private static String readSlstrL1BPixelGeoCodings = "s3tbx.reader.slstrl1b.pixelGeoCodings";
    private static String readOlciPixelGeoCoding = "s3tbx.reader.olci.pixelGeoCoding";
    private static String readMerisPixelGeoCoding = "s3tbx.reader.meris.pixelGeoCoding";
    private static String loadProfileTiePointData = "s3tbx.reader.loadProfileTiePoints";

    private javax.swing.JCheckBox slstrL1BPixelGeocodingsCheckBox;
    private javax.swing.JCheckBox olciPixelGeocodingsCheckBox;
    private javax.swing.JCheckBox merisPixelGeocodingsCheckBox;
    private javax.swing.JCheckBox loadProfileTiePointsCheckBox;

    S3ReaderOptionsPanel(final S3ReaderOptionsPanelController controller) {
        initComponents();
        // listen to changes in form fields and call controller.changed()
        slstrL1BPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        olciPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        merisPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        loadProfileTiePointsCheckBox.addItemListener(e -> controller.changed());
    }

    private void initComponents() {
        slstrL1BPixelGeocodingsCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(slstrL1BPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL1BPixelGeocodingsCheckBox.text")); // NOI18N
        olciPixelGeocodingsCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(olciPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.olciPixelGeocodingsCheckBox.text")); // NOI18N
        merisPixelGeocodingsCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(merisPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.merisPixelGeocodingsCheckBox.text")); // NOI18N
        loadProfileTiePointsCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(loadProfileTiePointsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.loadProfileTiepointsCheckBox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addComponent(slstrL1BPixelGeocodingsCheckBox)
                                                            .addGap(0, 512, Short.MAX_VALUE)
                                                            .addComponent(olciPixelGeocodingsCheckBox)
                                                            .addGap(0, 512, Short.MAX_VALUE)
                                                            .addComponent(merisPixelGeocodingsCheckBox)
                                                            .addGap(0, 512, Short.MAX_VALUE)
                                                            .addComponent(loadProfileTiePointsCheckBox))
                                          .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addComponent(slstrL1BPixelGeocodingsCheckBox)
                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                          .addComponent(olciPixelGeocodingsCheckBox)
                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                          .addComponent(merisPixelGeocodingsCheckBox)
                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                          .addComponent(loadProfileTiePointsCheckBox)
                                          .addContainerGap())
        );
    }

    void load() {
        slstrL1BPixelGeocodingsCheckBox.setSelected(
                Config.instance().preferences().getBoolean(readSlstrL1BPixelGeoCodings, false));
        olciPixelGeocodingsCheckBox.setSelected(
                Config.instance().preferences().getBoolean(readOlciPixelGeoCoding, false));
        merisPixelGeocodingsCheckBox.setSelected(
                Config.instance().preferences().getBoolean(readMerisPixelGeoCoding, false));
        loadProfileTiePointsCheckBox.setSelected(
                Config.instance().preferences().getBoolean(loadProfileTiePointData, false));
    }

    void store() {
        final Preferences preferences = Config.instance().preferences();
        preferences.putBoolean(
                readSlstrL1BPixelGeoCodings, slstrL1BPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(
                readOlciPixelGeoCoding, olciPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(
                readMerisPixelGeoCoding, merisPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(
                loadProfileTiePointData, loadProfileTiePointsCheckBox.isSelected());
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    boolean valid() {
        // Check whether form is consistent and complete
        return true;
    }

}
