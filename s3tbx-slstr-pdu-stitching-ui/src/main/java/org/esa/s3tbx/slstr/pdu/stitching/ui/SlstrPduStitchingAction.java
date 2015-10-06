package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.framework.ui.ModelessDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * @author Tonio Fincke
 */
@ActionID(category = "Processing", id = "org.esa.s3tbx.slstr.pdu.stitching.ui.SlstrPduStitchingAction")
@ActionRegistration(displayName = "#CTL_SlstrPduStitchingAction_Name", lazy = false)
//@ActionReference(path = "Menu/Raster/Geometric Operations", position = 10)
@NbBundle.Messages({
        "CTL_SlstrPduStitchingAction_Name=SLSTR PDU Stitching",
        "CTL_SlstrPduStitchingAction_Description=Stitches multiple Sentinel-3 SLSTR L1B products into a single one.",
        "CTL_SlstrPduStitchingAction_Help=pduStitchingAction"
})

public class SlstrPduStitchingAction extends AbstractSnapAction {

    private ModelessDialog dialog;

    public SlstrPduStitchingAction() {
        putValue(NAME, Bundle.CTL_SlstrPduStitchingAction_Name());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_SlstrPduStitchingAction_Description());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new SlstrPduStitchingDialog(Bundle.CTL_SlstrPduStitchingAction_Name(),
                                                 Bundle.CTL_SlstrPduStitchingAction_Help(), getAppContext());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Slstr.PDU.Stitching") != null;
    }
}
