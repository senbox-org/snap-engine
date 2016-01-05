/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.idepix.ui.actions;

import org.esa.s3tbx.idepix.algorithms.modis.ModisOp;
import org.esa.s3tbx.idepix.ui.IdepixDefaultDialog;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Idepix action for MODIS algorithm.
 *
 * @author Olaf Danne
 */
@ActionID(category = "Processing", id = "org.esa.s3tbx.idepix.ui.actions.IdepixModisAction")
@ActionRegistration(displayName = "#CTL_IdepixModisAction_Text")
@ActionReference(path = "Menu/Optical/Preprocessing/IdePix Pixel Classification", position = 200)
@NbBundle.Messages({"CTL_IdepixModisAction_Text=Modis"})
public class IdepixModisAction extends AbstractSnapAction {

    private static final String HELP_ID = "idepixPixelIdentification";

    public IdepixModisAction() {
        putValue(Action.SHORT_DESCRIPTION, "Performs pixel classification on a MODIS data product.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final OperatorMetadata opMetadata = ModisOp.class.getAnnotation(OperatorMetadata.class);
        final IdepixDefaultDialog dialog = new IdepixDefaultDialog(opMetadata.alias(),
                                                                   getAppContext(),
                                                                   "Idepix - Pixel Identification and Classification (MODIS mode)",
                                                                   HELP_ID,
                                                                   "_idepix");
        dialog.getJDialog().pack();
        dialog.show();
    }
}
