/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.common.reproject;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertySetDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 * <p>
 * Support for operator parameters input/output.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class OperatorParameterSupport {

    private static final ParameterUpdater DEFAULT = new ParameterUpdater() {
        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) {
        }
    };

    private ParameterDescriptorFactory descriptorFactory;
    private Map<String, Object> parameterMap;
    private PropertySet propertySet;
    private ParameterUpdater parameterUpdater;
    private PropertySetDescriptor propertySetDescriptor;
    private Class<? extends Operator> operatorType;

    /**
     * Creates a parameter support for the operator described by the given {@link OperatorDescriptor}.
     *
     * @param operatorDescriptor The operator descriptor.
     */
    public OperatorParameterSupport(OperatorDescriptor operatorDescriptor) {
        this(operatorDescriptor, null, null, null);
    }

    /**
     * Creates a parameter support for the operator described by the given {@link OperatorDescriptor}.
     * <p>
     * If a property set and a parameter map are given the client as to keep them in sync.
     * The {@code parameterUpdater} will be called before each save  and after each load request to
     * enable custom updating.
     *
     * @param operatorDescriptor The operator descriptor.
     * @param propertySet      The property set (can be null). If supplied a parameter map is required as well.
     * @param parameterMap     the parameter map (can be null)
     * @param parameterUpdater The parameter updater (can be null)
     *
     */
    public OperatorParameterSupport(OperatorDescriptor operatorDescriptor,
                                    PropertySet propertySet,
                                    Map<String, Object> parameterMap,
                                    ParameterUpdater parameterUpdater) {
        Assert.notNull(operatorDescriptor, "operatorDescriptor");
        init(null, operatorDescriptor, propertySet, parameterMap, parameterUpdater);
    }

    /**
     * @deprecated since BEAM 5, use {@link #OperatorParameterSupport(OperatorDescriptor)} instead
     */
    @Deprecated
    public OperatorParameterSupport(Class<? extends Operator> opType) {
        this(opType, null, null, null);
    }

    /**
     * @deprecated since BEAM 5, use {@link #OperatorParameterSupport(OperatorDescriptor, PropertySet, Map, ParameterUpdater)}
     */
    @Deprecated
    public OperatorParameterSupport(Class<? extends Operator> opType,
                                    PropertySet propertySet,
                                    Map<String, Object> parameterMap,
                                    ParameterUpdater parameterUpdater) {
        Assert.notNull(opType, "opType");
        init(opType, null, propertySet, parameterMap, parameterUpdater);
    }

    private void init(Class<? extends Operator> opType,
                      OperatorDescriptor operatorDescriptor,
                      PropertySet propertySet,
                      Map<String, Object> parameterMap,
                      ParameterUpdater parameterUpdater) {
        Assert.argument(parameterMap != null || propertySet == null, "parameterMap != null || propertySet == null");

        this.descriptorFactory = new ParameterDescriptorFactory();

        if (parameterMap == null) {
            parameterMap = new HashMap<>();
        }
        this.parameterMap = parameterMap;

        this.operatorType = opType != null ? opType : operatorDescriptor.getOperatorClass();

        if (propertySet == null) {
            if (operatorDescriptor != null) {
                try {
                    propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor, descriptorFactory.getSourceProductMap());
                } catch (ConversionException e) {
                    throw new IllegalStateException("Not able to init OperatorParameterSupport.", e);
                }
                propertySet = PropertyContainer.createMapBacked(this.parameterMap, propertySetDescriptor);
                propertySet.setDefaultValues();
            } else {
                propertySetDescriptor = DefaultPropertySetDescriptor.createFromClass(operatorType, descriptorFactory);
                propertySet = PropertyContainer.createMapBacked(this.parameterMap, propertySetDescriptor);
                propertySet.setDefaultValues();
            }
        }
        this.propertySet = propertySet;

        if (parameterUpdater == null) {
            parameterUpdater = DEFAULT;
        }
        this.parameterUpdater = parameterUpdater;

    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    /**
     * @deprecated since BEAM 5, use {@link #getPropertySet()}
     */
    @Deprecated
    public PropertySet getPopertySet() {
        return propertySet;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void fromDomElement(DomElement parametersElement) throws ValidationException, ConversionException {
        parameterMap.clear();
        propertySet.setDefaultValues();
        DefaultDomConverter domConverter = createDomConverter();
        domConverter.convertDomToValue(parametersElement, propertySet);
        parameterUpdater.handleParameterLoadRequest(parameterMap);
    }

    public DomElement toDomElement() throws ValidationException, ConversionException {
        parameterUpdater.handleParameterSaveRequest(parameterMap);
        DefaultDomConverter domConverter = createDomConverter();
        DefaultDomElement parametersElement = new DefaultDomElement("parameters");
        domConverter.convertValueToDom(propertySet, parametersElement);
        return parametersElement;
    }

    private DefaultDomConverter createDomConverter() {
        return new DefaultDomConverter(operatorType, descriptorFactory, propertySetDescriptor);
    }
}