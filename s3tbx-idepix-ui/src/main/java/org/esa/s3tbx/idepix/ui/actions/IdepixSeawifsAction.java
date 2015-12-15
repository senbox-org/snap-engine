package org.esa.s3tbx.idepix.ui.actions;

import org.esa.s3tbx.idepix.algorithms.seawifs.SeaWifsOp;
import org.esa.s3tbx.idepix.ui.IdepixDefaultDialog;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.rcp.actions.AbstractSnapAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Idepix action for  MODIS/SeaWiFS algorithm.
 *
 * @author Olaf Danne
 */
public class IdepixSeawifsAction extends AbstractSnapAction {

    public IdepixSeawifsAction() {
        setHelpId("idepix");
        putValue(Action.SHORT_DESCRIPTION, "Performs pixel classification on a SeaWiFS data product.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final IdepixDefaultDialog dialog =
                new IdepixDefaultDialog(OperatorSpi.getOperatorAlias(SeaWifsOp.class),
                                        getAppContext(),
                                        "IDEPIX Pixel Identification Tool - SeaWiFS" +
                                                " Algorithm",
                                        "idepixChain","");
        dialog.show();
    }
}
