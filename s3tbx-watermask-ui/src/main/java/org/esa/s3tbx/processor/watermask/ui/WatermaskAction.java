package org.esa.s3tbx.processor.watermask.ui;

import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(
        category = "Processing",
        id = "org.esa.s3tbx.processor.watermask.ui.WatermaskAction"
)
@ActionRegistration(displayName = "#CTL_WatermaskAction_Text")
@ActionReference(
        path = "Menu/Optical/Preprocessing",
        position = 300
)
@NbBundle.Messages({"CTL_WatermaskAction_Text=Land/Water Mask"})
public class WatermaskAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "LandWaterMask";
    private static final String HELP_ID = "watermaskScientificTool";

    public WatermaskAction() {
        putValue(SHORT_DESCRIPTION, "Provides an API as well as an operator for creating an accurate, shapefile-based land-water mask.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AppContext appContext = getAppContext();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OPERATOR_ALIAS, appContext,
                                                                                             "Land/Water Mask",
                                                                                             HELP_ID);
        dialog.setTargetProductNameSuffix("_watermask");
        dialog.getJDialog().pack();
        dialog.show();
    }
}
