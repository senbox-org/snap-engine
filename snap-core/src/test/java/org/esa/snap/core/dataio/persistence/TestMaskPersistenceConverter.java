/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.dataio.persistence;

import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;

import java.awt.*;

class TestMaskPersistenceConverter extends PersistenceConverter<Mask> {

    private final static String ID = "TestMaskPersistenceConverter:2";

    public String getID() {
        return ID;
    }

    private static final String[] EXPECTED_PROP_NAMES = new String[]{
            "name",
            "expression",
            "description",
            "color_rgb",
            "image_transparency"
    };

    public static final HistoricalDecoder[] HISTORICAL_DECODERS = {
            new HistoricalDecoder0(),
            new HistoricalDecoder1(),
    };

    @Override
    public Mask decodeImpl(Item item, Product product) {
        final Property<?>[] props = getPropertiesValidated(item);
        final String maskName = props[0].getValueString();
        final String expression = props[1].getValueString();
        final String description = props[2].getValueString();
        final Integer[] rgb = props[3].getValueInts();
        final Double transparency = props[4].getValueDouble();

        return product.addMask(
                maskName,
                expression,
                description,
                new Color(rgb[0], rgb[1], rgb[2]),
                transparency);
    }

    @Override
    public Item encode(Mask mask) {
        final Container container = new Container("mask");
        container.add(new Property<>(KEY_PERSISTENCE_ID, getID()));
        container.add(new Property<>("name", mask.getName()));
        container.add(new Property<>("expression", Mask.BandMathsType.getExpression(mask)));
        container.add(new Property<>("description", mask.getDescription()));
        final Color cl = mask.getImageColor();
        final int[] rgb = new int[]{cl.getRed(), cl.getGreen(), cl.getBlue()};
        container.add(new Property<>("color_rgb", rgb));
        container.add(new Property<>("image_transparency", mask.getImageTransparency()));
        return container;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return HISTORICAL_DECODERS;
    }

    private static Property<?>[] getPropertiesValidated(Item item) {
        final String message = validate(item);
        if (message != null) {
            throw new IllegalArgumentException(message);
        }
        return getProperties(item);
    }

    public static String validate(Item item) {
        if (!item.isContainer()) {
            return "Container expected.";
        }
        final String itemName = item.getName();
        if (!"mask".equals(itemName)) {
            return "Item with name 'mask' expected but was '" + itemName + "'.";
        }
        final Container container = item.asContainer();
        for (String propName : EXPECTED_PROP_NAMES) {
            final Property property = container.getProperty(propName);
            if (property == null) {
                return "Property '" + propName + "' expected.";
            }
        }
        return null;
    }

    public static Property[] getProperties(Item item) {
        final Container container = item.asContainer();
        final Property[] props = new Property[EXPECTED_PROP_NAMES.length];
        for (int i = 0; i < EXPECTED_PROP_NAMES.length; i++) {
            final String propName = EXPECTED_PROP_NAMES[i];
            final Property property = container.getProperty(propName);
            props[i] = property;
        }
        return props;
    }

    private static class HistoricalDecoder0 extends DimapHistoricalDecoder {

        @Override
        public boolean canDecode(Item item) {
            if (item == null || !item.isContainer()) {
                return false;
            }
            final String itemName = item.getName();
            if (!"mask".equals(itemName)) {
                return false;
            }
            final Container container = item.asContainer();
            for (String propName : new String[]{
                    "name",
                    "expr",
                    "description",
                    "color_rgb",
                    "image_transparency"
            }) {
                final Property property = container.getProperty(propName);
                if (property == null) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container container = item.asContainer();
            container.add(new Property<>(KEY_PERSISTENCE_ID, HistoricalDecoder1.ID));
            return item;
        }
    }

    private static class HistoricalDecoder1 implements HistoricalDecoder {

        public static final String ID = "MyMaskPersistenceConverter:1";

        @Override
        public String getID() {
            return ID;
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container container = item.asContainer();
            container.removeProperty(KEY_PERSISTENCE_ID);
            container.add(new Property<>(KEY_PERSISTENCE_ID, TestMaskPersistenceConverter.ID));
            Property<?> property = container.removeProperty("expr");
            container.add(new Property<>("expression", property.getValueString()));
            return item;
        }
    }
}
