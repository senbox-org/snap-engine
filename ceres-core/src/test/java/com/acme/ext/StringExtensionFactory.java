package com.acme.ext;

import com.bc.ceres.core.ExtensionFactory;

public class StringExtensionFactory implements ExtensionFactory {

    @Override
    public Object getExtension(Object object, Class<?> extensionType) {
        return new StringExtension1((String) object);
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[] {String.class};
    }
}
