/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.aatsr.sst.ui;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(
        category = "Processing",
        id = "org.esa.snap.aatsr.sst.ui.AatsrSstAction"
)
@ActionRegistration(displayName = "#CTL_AatsrSstActionText")
@ActionReference(
        path = "Menu/Optical/Thematic Water Processing",
        position = 100
)
@NbBundle.Messages({"CTL_AatsrSstActionText=(A)ATSR SST Processor"})
public class AatsrSstAction extends AbstractSnapAction {

    public static final String HELP_ID = "sstScientificTool";
    private HelpCtx helpCtx;

    public AatsrSstAction() {
        helpCtx = new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final String title = "(A)ATSR SST Processor";

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog("Aatsr.SST",
                                                                                             getAppContext(),
                                                                                             title, HELP_ID);
        final BindingContext bindingContext = dialog.getBindingContext();
        bindingContext.bindEnabledState("dual", true, "nadir", true);
        bindingContext.bindEnabledState("dualCoefficientsFile", true, "dual", true);
        bindingContext.bindEnabledState("dualMaskExpression", true, "dual", true);
        bindingContext.bindEnabledState("nadir", true, "dual", true);
        bindingContext.bindEnabledState("nadirCoefficientsFile", true, "nadir", true);
        bindingContext.bindEnabledState("nadirMaskExpression", true, "nadir", true);

        dialog.setTargetProductNameSuffix("_sst");
        dialog.getJDialog().pack();
        dialog.show();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }
}
