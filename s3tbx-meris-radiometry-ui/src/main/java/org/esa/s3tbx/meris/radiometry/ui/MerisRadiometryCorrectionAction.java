/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.meris.radiometry.ui;

import org.esa.s3tbx.meris.radiometry.MerisRadiometryCorrectionOp;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.Action;
import java.awt.event.ActionEvent;


@ActionID(category = "Processing", id = "org.esa.s3tbx.meris.radiometry.ui.MerisRadiometryCorrectionAction" )
@ActionRegistration(displayName = "#CTL_MerisRadiometryCorrectionAction_Text")
@ActionReference(path = "Menu/Optical/Preprocessing", position = 100 )
@NbBundle.Messages({"CTL_MerisRadiometryCorrectionAction_Text=MERIS Radiometric Correction"})
public class MerisRadiometryCorrectionAction extends AbstractSnapAction {

    public MerisRadiometryCorrectionAction() {
        setHelpId("merisRadiometryCorrection");
        putValue(Action.SHORT_DESCRIPTION, "Performs radiometric corrections on a MERIS L1b data product.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final OperatorMetadata opMetadata = MerisRadiometryCorrectionOp.class.getAnnotation(OperatorMetadata.class);
        final RadiometryDialog operatorDialog = new RadiometryDialog(opMetadata.alias(), getAppContext(),
                                                                     "MERIS L1b Radiometric Correction",
                                                                     getHelpId());
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }
}
