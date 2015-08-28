/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.dataio.s3.preferences.ui;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@OptionsPanelController.SubRegistration(
        location = "S3TBX",
        displayName = "#LBL_Sentinel3ToolboxReaderOption_DisplayName",
        keywords = "#LBL_Sentinel3ToolboxReaderOption_Keywords",
        keywordsCategory = "S3TBX"
)
@org.openide.util.NbBundle.Messages({
        "LBL_Sentinel3ToolboxReaderOption_DisplayName=Sentinel-3 Readers",
        "LBL_Sentinel3ToolboxReaderOption_Keywords=s3tbx"
})
public final class S3ReaderOptionsPanelController extends OptionsPanelController {

    private S3ReaderOptionsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    public void update() {
        getPanel().load();
        changed = false;
    }

    public void applyChanges() {
        SwingUtilities.invokeLater(() -> {
            getPanel().store();
            changed = false;
        });
    }

    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    public boolean isValid() {
        return getPanel().valid();
    }

    public boolean isChanged() {
        return changed;
    }

    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private S3ReaderOptionsPanel getPanel() {
        if (panel == null) {
            panel = new S3ReaderOptionsPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

}
