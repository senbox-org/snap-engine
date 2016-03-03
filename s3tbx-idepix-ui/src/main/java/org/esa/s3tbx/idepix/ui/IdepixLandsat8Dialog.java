package org.esa.s3tbx.idepix.ui;

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.s3tbx.idepix.algorithms.landsat8.NNSelector;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.internal.RasterDataNodeValues;
import org.esa.snap.core.gpf.ui.*;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 19.11.2015
 * Time: 10:43
 *
 * @author olafd
 */
public class IdepixLandsat8Dialog extends SingleTargetProductDialog {

    private final String operatorName;
    private final OperatorDescriptor operatorDescriptor;
    private DefaultIOParametersPanel ioParametersPanel;
    private final OperatorParameterSupport parameterSupport;
    private final BindingContext bindingContext;

    private JTabbedPane form;
    private PropertyDescriptor[] rasterDataNodeTypeProperties;
    private String targetProductNameSuffix;
    private ProductChangedHandler productChangedHandler;
    private JPanel parametersPanel;
    private JComboBox nnSelectionComboBox;

    public IdepixLandsat8Dialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;
        targetProductNameSuffix = "";

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        operatorDescriptor = operatorSpi.getOperatorDescriptor();
        ioParametersPanel = new DefaultIOParametersPanel(getAppContext(), operatorDescriptor, getTargetProductSelector());

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        final PropertySet propertySet = parameterSupport.getPropertySet();
        bindingContext = new BindingContext(propertySet);

        if (propertySet.getProperties().length > 0) {
            if (!sourceProductSelectorList.isEmpty()) {
                Property[] properties = propertySet.getProperties();
                List<PropertyDescriptor> rdnTypeProperties = new ArrayList<>(properties.length);
                for (Property property : properties) {
                    PropertyDescriptor parameterDescriptor = property.getDescriptor();
                    if (parameterDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                        rdnTypeProperties.add(parameterDescriptor);
                    }
                }
                rasterDataNodeTypeProperties = rdnTypeProperties.toArray(
                        new PropertyDescriptor[rdnTypeProperties.size()]);
            }
        }
        productChangedHandler = new ProductChangedHandler();
        if (!sourceProductSelectorList.isEmpty()) {
            sourceProductSelectorList.get(0).addSelectionChangeListener(productChangedHandler);
        }
    }

    @Override
    public int show() {
        ioParametersPanel.initSourceProductSelectors();
        if (form == null) {
            initForm();
            if (getJDialog().getJMenuBar() == null) {
                final OperatorMenu operatorMenu = createDefaultMenuBar();
                getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
            }
        }
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        productChangedHandler.releaseProduct();
        ioParametersPanel.releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = ioParametersPanel.createSourceProductsMap();
        return GPF.createProduct(operatorName, parameterSupport.getParameterMap(), sourceProducts);
    }

    public String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    private void initForm() {
        form = new JTabbedPane();
        form.add("I/O Parameters", ioParametersPanel);

        if (bindingContext.getPropertySet().getProperties().length > 0) {
            final PropertyPane parametersPane = new PropertyPane(bindingContext);
            parametersPanel = parametersPane.createPanel();

            // identify nnSelectionComboBox component and set renderer/listener
            initNNSelectionComboBox();

            parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
            form.add("Processing Parameters", new JScrollPane(parametersPanel));
            updateSourceProduct();
        }
    }

    private void initNNSelectionComboBox() {
        if (parametersPanel != null && parametersPanel.getComponents() != null) {
            final Component[] components = parametersPanel.getComponents();
            for (int i = 0; i < components.length - 1; i++) {
                if (components[i].getName() != null && components[i].getName().equals("nnSelector")) {
                    if (nnSelectionComboBox == null) {
                        nnSelectionComboBox = (JComboBox) components[i];
                        nnSelectionComboBox.setRenderer(new NNSelectionRenderer());
                        nnSelectionComboBox.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                final NNSelector nnSelector = (NNSelector) nnSelectionComboBox.getSelectedItem();
                                updateSeparationDefaultValues(nnSelector, components);
                            }
                        });
                    }
                }
            }
        }
    }

    private void updateSeparationDefaultValues(NNSelector nnSelector, Component[] components) {
        for (int i = 0; i < components.length - 1; i++) {
            if (components[i].getName() != null && components[i].getName().equals("nnCloudAmbiguousLowerBoundaryValue")) {
                JTextField nnCloudAmbiguousLowerBoundaryValueTextField = (JTextField) components[i];
                nnCloudAmbiguousLowerBoundaryValueTextField.setText(String.valueOf(nnSelector.getSeparationValues()[0]));
            }
            if (components[i].getName() != null && components[i].getName().equals("nnCloudAmbiguousSureSeparationValue")) {
                JTextField nnCloudAmbiguousSureSeparationValueTextField = (JTextField) components[i];
                nnCloudAmbiguousSureSeparationValueTextField.setText(String.valueOf(nnSelector.getSeparationValues()[1]));
            }
            if (components[i].getName() != null && components[i].getName().equals("nnCloudSureSnowSeparationValue")) {
                JTextField nnCloudSureSnowSeparationValueTextField = (JTextField) components[i];
                nnCloudSureSnowSeparationValueTextField.setText(String.valueOf(nnSelector.getSeparationValues()[2]));
            }
        }
    }

    private OperatorMenu createDefaultMenuBar() {
        return new OperatorMenu(getJDialog(),
                                operatorDescriptor,
                                parameterSupport,
                                getAppContext(),
                                getHelpID());
    }

    private void updateSourceProduct() {
        try {
            Property property = bindingContext.getPropertySet().getProperty(UIUtils.PROPERTY_SOURCE_PRODUCT);
            if (property != null) {
                property.setValue(productChangedHandler.currentProduct);
            }
        } catch (ValidationException e) {
            throw new IllegalStateException("Property '" + UIUtils.PROPERTY_SOURCE_PRODUCT + "' must be of type " + Product.class + ".", e);
        }
    }


    private class ProductChangedHandler extends AbstractSelectionChangeListener implements ProductNodeListener {

        private Product currentProduct;

        public void releaseProduct() {
            if (currentProduct != null) {
                currentProduct.removeProductNodeListener(this);
                currentProduct = null;
                updateSourceProduct();
            }
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            if (selection != null) {
                final Product selectedProduct = (Product) selection.getSelectedValue();
                if (selectedProduct != null) {
                    String productType = selectedProduct.getProductType();
                    if (selectedProduct != currentProduct) {
                        if (currentProduct != null) {
                            currentProduct.removeProductNodeListener(this);
                        }
                        currentProduct = selectedProduct;
                        currentProduct.addProductNodeListener(this);
                        updateTargetProductName();
                        updateValueSets(currentProduct);
                        updateSourceProduct();
                    }
                }
            }
        }


        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleProductNodeEvent();
        }

        private void updateTargetProductName() {
            String productName = "";
            if (currentProduct != null) {
                productName = currentProduct.getName();
            }
            final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
            targetProductSelectorModel.setProductName(productName + getTargetProductNameSuffix());
        }

        private void handleProductNodeEvent() {
            updateValueSets(currentProduct);
        }

        private void updateValueSets(Product product) {
            if (rasterDataNodeTypeProperties != null) {
                for (PropertyDescriptor propertyDescriptor : rasterDataNodeTypeProperties) {
                    updateValueSet(propertyDescriptor, product);
                }
            }
        }

        private void updateValueSet(PropertyDescriptor propertyDescriptor, Product product) {
            String[] values = new String[0];
            if (product != null) {
                Object object = propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME);
                if (object != null) {
                    @SuppressWarnings("unchecked")
                    Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) object;
                    boolean includeEmptyValue = !propertyDescriptor.isNotNull() && !propertyDescriptor.isNotEmpty() &&
                            !propertyDescriptor.getType().isArray();
                    values = RasterDataNodeValues.getNames(product, rasterDataNodeType, includeEmptyValue);
                }
            }
            propertyDescriptor.setValueSet(new ValueSet(values));
        }
    }

    private class NNSelectionChangedHandler extends AbstractSelectionChangeListener {
        File currentNNFile;

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            if (selection != null) {
                final File selectedNNFile = (File) selection.getSelectedValue();
                if (selectedNNFile != null) {
                    if (selectedNNFile != currentNNFile) {
                        currentNNFile = selectedNNFile;
                    }
                    checkComponentsToChange(selectedNNFile);
                }
            }
        }

        private void checkComponentsToChange(File selectedNNFile) {
            System.out.println("CHANGE: selectedNNFile = " + selectedNNFile.getName());
        }
    }

    private static class NNSelectionRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            final Component cellRendererComponent =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (cellRendererComponent instanceof JLabel && value instanceof NNSelector) {
                final JLabel label = (JLabel) cellRendererComponent;
                final NNSelector nn = (NNSelector) value;
                label.setText(nn.getLabel());
            }

            return cellRendererComponent;
        }


    }
}
