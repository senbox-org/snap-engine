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

package org.esa.s3tbx.insitu.ui;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Tools", id = "org.esa.s3tbx.insitu.ui.InsituClientAction" )
@ActionRegistration(displayName = "#CTL_InsituClientAction_Text", iconBase = "org/esa/s3tbx/insitu/insitu.png")
@ActionReference(path = "Menu/Vector/Import", position = 500)
@NbBundle.Messages({"CTL_InsituClientAction_Text=In-Situ Client"})
public class InsituClientAction extends AbstractSnapAction {

    private static final String INSITU_TOOL_HELP_ID = "insituClientTool";

    public InsituClientAction() {
        putValue(SHORT_DESCRIPTION, "Access to remote in-situ data service");
        putValue(HELP_ID, INSITU_TOOL_HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new InsituClientDialog(getAppContext().getApplicationWindow(), "In-Situ Data Access", INSITU_TOOL_HELP_ID).show();
    }

}
