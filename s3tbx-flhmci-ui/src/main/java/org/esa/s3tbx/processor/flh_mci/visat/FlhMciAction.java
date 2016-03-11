package org.esa.s3tbx.processor.flh_mci.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@ActionID(category = "Processing", id = "org.esa.s3tbx.processor.flh_mci.visat.FlhMciAction" )
@ActionRegistration(displayName = "#CTL_FlhMciAction_Text")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 200 )
@NbBundle.Messages({"CTL_FlhMciAction_Text=FLH/MCI Processor"})
public class FlhMciAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "FlhMci";
    private static final String HELP_ID = "flhMciScientificTool";
    private static final String LOWER_BASELINE_BAND_NAME = "lowerBaselineBandName";
    private static final String UPPER_BASE_LINE_BAND_NAME = "upperBaselineBandName";
    private static final String SIGNAL_BAND_NAME = "signalBandName";

    public FlhMciAction() {
        putValue(SHORT_DESCRIPTION, "Generates florescence line height (FLH) / maximum chlorophyll index (MCI) from spectral bands.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AppContext appContext = getAppContext();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OPERATOR_ALIAS, appContext,
                                                                                             Bundle.CTL_FlhMciAction_Text(),
                                                                                             HELP_ID);
        final BindingContext bindingContext = dialog.getBindingContext();
        final PropertySet propertySet = bindingContext.getPropertySet();
        configurePropertySet(propertySet);

        bindingContext.bindEnabledState("slopeBandName", true, "slope", true);
        bindingContext.addPropertyChangeListener("preset", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Presets preset = (Presets) evt.getNewValue();
                if (preset != Presets.NONE) {
                    setValueIfValid(propertySet, LOWER_BASELINE_BAND_NAME, preset.getLowerBaselineBandName());
                    setValueIfValid(propertySet, UPPER_BASE_LINE_BAND_NAME, preset.getUpperBaselineBandName());
                    setValueIfValid(propertySet, SIGNAL_BAND_NAME, preset.getSignalBandName());
                    propertySet.setValue("lineHeightBandName", preset.getLineHeightBandName());
                    propertySet.setValue("slopeBandName", preset.getSlopeBandName());
                    propertySet.setValue("maskExpression", preset.getMaskExpression());
                }
            }

            private void setValueIfValid(PropertySet propertySet, String propertyName, String bandName) {
                if (propertySet.getDescriptor(propertyName).getValueSet().contains(bandName)) {
                    propertySet.setValue(propertyName, bandName);
                }
            }
        });

        dialog.setTargetProductNameSuffix("_flhmci");
        dialog.getJDialog().pack();
        dialog.show();
    }

    private void configurePropertySet(PropertySet propertySet) {
        final PropertySet presetPropertySet = PropertyContainer.createObjectBacked(new PresetContainer());

        // awkward - purpose is to insert 'preset' property at the first position of the binding context's property set
        final Property[] properties = propertySet.getProperties();
        propertySet.removeProperties(properties);
        propertySet.addProperty(presetPropertySet.getProperty("preset"));
        propertySet.addProperties(properties);
    }

    private static class PresetContainer {

        private Presets preset = Presets.NONE;
    }
}
