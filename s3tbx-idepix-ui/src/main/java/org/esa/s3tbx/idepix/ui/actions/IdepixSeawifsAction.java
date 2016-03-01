package org.esa.s3tbx.idepix.ui.actions;

//import org.esa.s3tbx.idepix.algorithms.seawifs.SeaWifsOp;
//import org.esa.s3tbx.idepix.ui.IdepixDefaultDialog;
//import org.esa.snap.core.gpf.annotations.OperatorMetadata;
//import org.esa.snap.rcp.actions.AbstractSnapAction;
//import org.openide.awt.ActionID;
//import org.openide.awt.ActionReference;
//import org.openide.awt.ActionRegistration;
//import org.openide.util.NbBundle;
//
//import javax.swing.*;
//import java.awt.event.ActionEvent;
//
///**
// * Idepix action for SeaWiFS algorithm.
// *
// * @author Olaf Danne
// */
//@ActionID(category = "Processing", id = "org.esa.s3tbx.idepix.ui.actions.IdepixSeawifsAction")
//@ActionRegistration(displayName = "#CTL_IdepixSeawifsAction_Text")
//@ActionReference(path = "Menu/Optical/Preprocessing/IdePix Pixel Classification", position = 200)
//@NbBundle.Messages({"CTL_IdepixSeawifsAction_Text=SeaWiFS"})
//public class IdepixSeawifsAction extends AbstractSnapAction {
//
//    private static final String HELP_ID = "idepixTool";
//
//    public IdepixSeawifsAction() {
//        putValue(Action.SHORT_DESCRIPTION, "Performs pixel classification on a SeaWiFS data product.");
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        final OperatorMetadata opMetadata = SeaWifsOp.class.getAnnotation(OperatorMetadata.class);
//        final IdepixDefaultDialog dialog = new IdepixDefaultDialog(opMetadata.alias(),
//                                                                   getAppContext(),
//                                                                   "Idepix - Pixel Identification and Classification (SeaWiFS mode)",
//                                                                   HELP_ID,
//                                                                   "_idepix");
//        dialog.getJDialog().pack();
//        dialog.show();
//    }
//}

public class IdepixSeawifsAction {
    // todo: activate code above
}