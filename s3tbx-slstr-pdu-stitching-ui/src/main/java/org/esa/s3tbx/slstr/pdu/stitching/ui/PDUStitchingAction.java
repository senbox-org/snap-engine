package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * @author Tonio Fincke
 */

@ActionID(category = "Operators", id = "org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingAction")
@ActionRegistration(displayName = "#CTL_PDUStitchingAction_Name")
@ActionReference(path = "Menu/Optical/Geometric", position = 8)
@NbBundle.Messages("CTL_PDUStitchingAction_Name=SLSTR L1B PDU Stitching")
public class PDUStitchingAction extends AbstractSnapAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new PDUStitchingDialog(Bundle.CTL_PDUStitchingAction_Name(), "pduStitchingAction", getAppContext());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("PduStitching") != null;
    }

}
