package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
class PDUStitchingModel {

    public static final String PROPERTY_SOURCE_PRODUCTS = "sourceProducts";
    public static final String PROPERTY_SOURCE_PRODUCT_PATHS = "sourceProductPaths";
    public static final String PROPERTY_TARGET_DIR = "targetDir";

    private BindingContext bindingContext;
    private final PropertySet container;
    private final Map<String, Object> parameterMap = new HashMap<>();
    private boolean openInApp;

    PDUStitchingModel() {
        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("PduStitching", parameterMap);
        openInApp = false;
        addTransientProperty(PROPERTY_SOURCE_PRODUCTS, Product[].class);
    }

    private void addTransientProperty(String propertyName, Class<?> propertyType) {
        PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, propertyType);
        descriptor.setTransient(true);
        final Property property = new Property(descriptor, new MapEntryAccessor(parameterMap, propertyName));
        container.addProperty(property);
    }

    Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    PropertySet getPropertySet() {
        return container;
    }

    public Object getPropertyValue(String propertyName) {
        return container.getValue(propertyName);
    }

    public void setPropertyValue(String propertyName, Object value) {
        container.setValue(propertyName, value);
    }

    public void setOpenInApp(boolean openInApp) {
        this.openInApp = openInApp;
    }

    public boolean openInApp() {
        return openInApp;
    }

    public Product[] getSourceProducts() {
        final Product[] products = (Product[]) getPropertyValue(PROPERTY_SOURCE_PRODUCTS);
        if (products == null) {
            return new Product[0];
        }
        return products;
    }

    public BindingContext getBindingContext() {
        if (bindingContext == null) {
            bindingContext = new BindingContext(container);
        }
        return bindingContext;
    }

}
