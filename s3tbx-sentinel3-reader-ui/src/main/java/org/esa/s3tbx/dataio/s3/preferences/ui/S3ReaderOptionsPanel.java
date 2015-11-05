/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.dataio.s3.preferences.ui;

import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.meris.MerisProductFactory;
import org.esa.s3tbx.dataio.s3.olci.OlciProductFactory;
import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class S3ReaderOptionsPanel extends javax.swing.JPanel {

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
        final Preferences preferences = Config.instance("s3tbx").load().preferences();
        slstrL1BPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_USE_PIXELGEOCODINGS, false));
        olciPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(OlciProductFactory.OLCI_USE_PIXELGEOCODING, false));
        merisPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(MerisProductFactory.MERIS_SAFE_USE_PIXELGEOCODING, false));
        loadProfileTiePointsCheckBox.setSelected(
                preferences.getBoolean(AbstractProductFactory.LOAD_PROFILE_TIE_POINTS, false));
    }

    void store() {
        final Preferences preferences = Config.instance("s3tbx").load().preferences();
        preferences.putBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_USE_PIXELGEOCODINGS,
                               slstrL1BPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(OlciProductFactory.OLCI_USE_PIXELGEOCODING, olciPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(MerisProductFactory.MERIS_SAFE_USE_PIXELGEOCODING, merisPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(AbstractProductFactory.LOAD_PROFILE_TIE_POINTS, loadProfileTiePointsCheckBox.isSelected());
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
