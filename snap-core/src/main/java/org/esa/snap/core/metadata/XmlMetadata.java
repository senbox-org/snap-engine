/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.core.metadata;

import org.esa.snap.core.datamodel.ProductData;

/**
 * Base metadata class that exposes helper methods for easier access of interesting metadata values.
 *
 * @author Cosmin Cara
 */
public abstract class XmlMetadata extends GenericXmlMetadata {

    protected int width;
    protected int height;
    protected int numBands;

    /**
     * Constructs an instance of metadata class and assigns a name to the root <code>MetadataElement</code>.
     *
     * @param name The name of this instance, and also the initial name of the root element.
     */
    public XmlMetadata(String name) {
        super(name);
    }

    /**
     * Returns the number of bands of the product.
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return the number of bands
     */
    public abstract int getNumBands();

    /**
     * Returns the name of the product, as found in metadata.
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return The name of the product
     */
    public abstract String getProductName();

    /**
     * Returns the name of the raster format (for example: TIFF, NITF, etc).
     * <p>
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return The raster format name/code.
     */
    public abstract String getFormatName();

    /**
     * Returns the width of the product, in pixels.
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return The width (in pixels) of the product.
     */
    public abstract int getRasterWidth();

    /**
     * Returns the height of the product, in pixels.
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return The height (in pixels) of the product.
     */
    public abstract int getRasterHeight();

    /**
     * Returns the names of raster files for the product.
     * This getter should be overridden in all derived classes because
     * each metadata type may have a different hierarchy of nodes for
     * getting this value.
     *
     * @return An array of raster file names.
     */
    public abstract String[] getRasterFileNames();

    /**
     * Sets the name of this metadata (and also the name of the root element).
     *
     * @param value The name of the metadata file.
     */
    public void setName(String value) {
        this.name = value;
        this.rootElement.setName(value);
    }

    /**
     * Returns the product start time in UTC format.
     * This method has to be implemented by subclasses since the value may be or may not be present in metadata,
     * or may be in different formats for different products.
     * @return  the product start time
     */
    public abstract ProductData.UTC getProductStartTime();

    /**
     * Returns the product end time in UTC format.
     * This method has to be implemented by subclasses since the value may be or may not be present in metadata,
     * or may be in different formats for different products.
     * @return  the product end time
     */
    public abstract ProductData.UTC getProductEndTime();

    /**
     * Returns the product center time in UTC format.
     * This method has to be implemented by subclasses since the value may be or may not be present in metadata,
     * or may be in different formats for different products.
     * @return  the product center time.
     */
    public abstract ProductData.UTC getCenterTime();

    /**
     * Returns the product description, preferably from metadata
     * @return  the product description
     */
    public abstract String getProductDescription();

    /**
     * Converts the value to a float array.
     * @param value The string list of values
     * @param separator The items separator
     * @return  An array of float values
     */
    protected float[] asFloatArray(String value, String separator) {
        float[] array = null;
        if (value != null && !value.isEmpty()) {
            String[] values = value.split(separator);
            if (values.length > 1) {
                array = new float[values.length];
                for (int i = 0; i < values.length; i++) {
                    array[i] = Float.parseFloat(values[i]);
                }
            }
        }
        return array;
    }

    /**
     * Silently converts a string to a float (i.e. without throwing exception for unconvertible values)
     * @param value The value to be converted
     * @return  The float value or <code>Float.NaN</code> if the conversion cannot be performed.
     */
    protected float asFloat(String value) {
        float ret = Float.NaN;
        try {
            ret = Float.parseFloat(value);
        } catch (NumberFormatException ignored) {}
        return ret;
    }

    /**
     * Silently converts a string to a int (i.e. without throwing exception for unconvertible values)
     * @param value The value to be converted
     * @return  The int value or <code>0</code> if the conversion cannot be performed.
     */
    protected int asInt(String value) {
        int ret = 0;
        try {
            ret = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        return ret;
    }
}
