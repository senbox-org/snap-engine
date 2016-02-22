package org.esa.s3tbx.processor.rad2refl.ui;

import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Processing", id = "org.esa.s3tbx.processor.rad2refl.ui.Rad2ReflAction" )
@ActionRegistration(displayName = "#CTL_Rad2ReflAction_Text")
@ActionReference(path = "Menu/Optical/Preprocessing", position = 400 )
@NbBundle.Messages({"CTL_Rad2ReflAction_Text=Radiance-to-Reflectance Processor"})
public class Rad2ReflAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "Rad2Refl";
    private static final String HELP_ID = "rad2ReflScientificTool";

    public Rad2ReflAction() {
        putValue(SHORT_DESCRIPTION, "The Radiance-to-Reflectance Processor provides conversion from radiances to reflectances or backwards.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AppContext appContext = getAppContext();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OPERATOR_ALIAS, appContext,
                                                                                             Bundle.CTL_Rad2ReflAction_Text(),
                                                                                             HELP_ID);

        dialog.setTargetProductNameSuffix("_radrefl");
        dialog.getJDialog().pack();
        dialog.show();
    }

}
