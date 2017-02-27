/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.fub.wew.ui;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * Action for starting the FUB water processor operator user interface.
 *
 * @author Tonio Fincke
 */
@ActionID(category = "Processing", id = "org.esa.s3tbx.wew.water.ui.WaterProcessorOpAction")
@ActionRegistration(displayName = "#CTL_WaterProcessorOpAction_Text")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 1100)
@NbBundle.Messages({"CTL_WaterProcessorOpAction_Text=MERIS FUB/WeW Water Processor"})
public class WaterProcessorOpAction extends AbstractSnapAction {
    private String operatorName = "FUB.Water";
    private String targetProductNameSuffix = "_wew";
    private static final String Help_ID = "fubWeWTool";
    private ModelessDialog dialog;


    @Override
    public void actionPerformed(ActionEvent event) {
        if (dialog == null) {
            WaterProcessorDialog dialog = new WaterProcessorDialog(operatorName, getAppContext(),
                                                                   Bundle.CTL_WaterProcessorOpAction_Text(),
                                                                   Help_ID);
            dialog.setTargetProductNameSuffix(targetProductNameSuffix);
            this.dialog = dialog;
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName) != null;
    }

}
