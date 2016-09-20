package org.esa.s3tbx.fub.wew.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.internal.AbstractButtonAdapter;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductExpressionPane;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A panel which displays the parameters needed by the FUB water processor operator
 *
 * @author Tonio Fincke
 */
class WaterVariablesPanel extends JPanel {

    private final AppContext appContext;
    private BindingContext bindingContext;

    WaterVariablesPanel(AppContext appContext, BindingContext bindingContext) {
        this.appContext = appContext;
        this.bindingContext = bindingContext;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(this, createCheckBoxComponent(WaterFormConstants.PROPERTY_KEY_COMPUTE_CHL), gbc, "insets=5,fill=HORIZONTAL,weightx=1.0,weighty=1.0,gridy=0");
        GridBagUtils.addToPanel(this, createCheckBoxComponent(WaterFormConstants.PROPERTY_KEY_COMPUTE_TSM), gbc, "gridy=1");
        GridBagUtils.addToPanel(this, createCheckBoxComponent(WaterFormConstants.PROPERTY_KEY_COMPUTE_YS), gbc, "gridy=2");
        GridBagUtils.addToPanel(this, createCheckBoxComponent(WaterFormConstants.PROPERTY_KEY_COMPUTE_ATMO), gbc, "gridy=3");
        GridBagUtils.addToPanel(this, createCheckBoxComponent(WaterFormConstants.PROPERTY_KEY_CHECK_SUSPECT), gbc, "gridy=4");
        GridBagUtils.addToPanel(this, createValidExpressionPanel(), gbc, "gridy=5,fill=HORIZONTAL,weightx=1.0,weighty=0.0");
        GridBagUtils.addVerticalFiller(this, gbc);
    }

    private JCheckBox createCheckBoxComponent(String propertyName) {
        final Property property = bindingContext.getPropertySet().getProperty(propertyName);
        JCheckBox checkBox = new JCheckBox(property.getDescriptor().getDisplayName());
        checkBox.setSelected((Boolean) property.getDescriptor().getDefaultValue());
        ComponentAdapter adapter = new AbstractButtonAdapter(checkBox);
        bindingContext.bind(property.getDescriptor().getName(), adapter);
        return checkBox;
    }

    private JPanel createValidExpressionPanel() {
        final JButton button = new JButton("...");
        final Dimension preferredSize = button.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        button.setPreferredSize(preferredSize);
        button.setEnabled(hasSourceProducts());

        bindingContext.getPropertySet().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!evt.getPropertyName().equals(WaterFormConstants.PROPERTY_KEY_SOURCE_PRODUCT)) {
                    return;
                }
                button.setEnabled(hasSourceProducts());
            }
        });
        final Property property = bindingContext.getPropertySet().getProperty(WaterFormConstants.PROPERTY_KEY_EXPRESSION);
        final JTextField textField = new JTextField();
        // workaround for http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7027598
        textField.setDropTarget(null);

        textField.setText(property.getDescriptor().getDefaultValue().toString());
        bindingContext.bind(property.getDescriptor().getName(), textField);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final String expression = editExpression(textField.getText());
                if (expression != null) {
                    textField.setText(expression);
                    try {
                        bindingContext.getPropertySet().getProperty(WaterFormConstants.PROPERTY_KEY_EXPRESSION).setValue(expression);
                    } catch (ValidationException e) {
                        appContext.handleError("Invalid expression", e);
                    }
                }
            }
        });

        final JPanel validExpressionPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(validExpressionPanel, new JLabel("Valid expression:"), gbc, "anchor=NORTHWEST,insets=3,insets.top=6");
        GridBagUtils.addToPanel(validExpressionPanel, textField, gbc, "gridx=1,weightx=1,fill=HORIZONTAL,insets.top=3,insets.left=24");
        GridBagUtils.addToPanel(validExpressionPanel, button, gbc, "gridx=2,weightx=0,fill=NONE,insets.top=2,insets.left=3");

        return validExpressionPanel;
    }

    private boolean hasSourceProducts() {
        return getSourceProducts().length > 0;
    }

    public Product[] getSourceProducts() {
        final Property property = bindingContext.getPropertySet().getProperty(WaterFormConstants.PROPERTY_KEY_SOURCE_PRODUCT);
        if (property == null) {
            return new Product[0];
        }
        return (Product[]) property.getValue();
    }

    private String editExpression(String expression) {
        final Product product = getSourceProducts()[0];
        if (product == null) {
            return null;
        }
        final ProductExpressionPane expressionPane;
        expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                appContext.getPreferences());
        expressionPane.setCode(expression);
        final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return expressionPane.getCode();
        }
        return null;
    }

}
