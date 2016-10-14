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

package org.esa.s3tbx.arc.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.param.validators.StringArrayValidator;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

@ActionID(category = "Processing", id = "org.esa.snap.arc.ui.ArcAction" )
@ActionRegistration(displayName = "#CTL_ArcActionText")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 110 )
@NbBundle.Messages({"CTL_ArcActionText=ARC SST Processor"})
public class ArcAction extends AbstractSnapAction {

    public static final String HELP_ID = "arcScientificTool";
    private HelpCtx helpCtx;

    public ArcAction() {
        helpCtx = new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        final ArcSingleTargetProductDialog dialog = new ArcSingleTargetProductDialog();

        final BindingContext bindingContext = dialog.getBindingContext();
        bindingContext.bindEnabledState("asdiCoefficientsFile", true, "asdi", true);
        bindingContext.bindEnabledState("asdiMaskExpression", true, "asdi", true);
        bindingContext.bindEnabledState("dual", true, "nadir", true);
        bindingContext.bindEnabledState("dualCoefficientsFile", true, "dual", true);
        bindingContext.bindEnabledState("dualMaskExpression", true, "dual", true);
        bindingContext.bindEnabledState("nadir", true, "dual", true);
        bindingContext.bindEnabledState("nadirCoefficientsFile", true, "nadir", true);
        bindingContext.bindEnabledState("nadirMaskExpression", true, "nadir", true);

        ArrayList<SourceProductSelector> selectorList = dialog.getDefaultIOParametersPanel().getSourceProductSelectorList();
        if (!selectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = selectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
                @Override
                public void selectionChanged(SelectionChangeEvent event) {
                    final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                    final PropertySet propertySet = bindingContext.getPropertySet();
                    if (selectedProduct != null) {
                        try {
                            final Presets preset = Presets.valueOf(selectedProduct.getProductType());
                            propertySet.setValue("asdiMaskExpression", preset.getMask());
                            propertySet.setValue("nadirMaskExpression", preset.getMask());
                            propertySet.setValue("dualMaskExpression", preset.getMask());
                            propertySet.setValue("asdi", preset.getAsdi());
                        } catch (IllegalArgumentException ex) {
                            // Unrecognised product type
                        }
                    }
                }
            });
        }
        dialog.setTargetProductNameSuffix("_arc");
        dialog.getJDialog().pack();
        dialog.show();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    private class ArcSingleTargetProductDialog extends DefaultSingleTargetProductDialog {

        public ArcSingleTargetProductDialog() {
            super("Arc.SST", ArcAction.this.getAppContext(), Bundle.CTL_ArcActionText(), ArcAction.HELP_ID);
        }

        @Override
        public DefaultIOParametersPanel getDefaultIOParametersPanel() {
            return super.getDefaultIOParametersPanel();
        }
    }

    private enum Presets {
        AT1_TOA_1P("!cloud_flags_nadir.LAND", true),
        AT2_TOA_1P("!cloud_flags_nadir.LAND", true),
        ATS_TOA_1P("!cloud_flags_nadir.LAND", true),
        SL_1_RBT("confidence_in_ocean", false);

        private final String mask;
        private final boolean asdi;

        private Presets(String mask, boolean asdi) {
            this.mask = mask;
            this.asdi = asdi;
        }

        String getMask() { return this.mask; }
        boolean getAsdi() { return this.asdi; }
    }

}
