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

package org.esa.snap.jp2.reader.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kraftek on 7/15/2015.
 */
public class ImageInfo {

    private int x0;
    private int y0;
    private int width;
    private int height;
    private List<ImageInfoComponent> components;

    public ImageInfo() {
        components = new ArrayList<>();
    }

    public int getX0() {
        return x0;
    }

    public void setX0(int x0) {
        this.x0 = x0;
    }

    public int getY0() {
        return y0;
    }

    public void setY0(int y0) {
        this.y0 = y0;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<ImageInfoComponent> getComponents() {
        return components;
    }

    public void addComponent(int dx, int dy, int precision, boolean isSigned) {
        components.add(new ImageInfoComponent(dx, dy, precision, isSigned));
    }

    public int getNumComponents() {
        return components.size();
    }

    public MetadataElement toMetadataElement() {
        MetadataElement element = new MetadataElement("Image Info");
        element.addAttribute(new MetadataAttribute("x0", ProductData.ASCII.createInstance(String.valueOf(x0)), false));
        element.addAttribute(new MetadataAttribute("y0", ProductData.ASCII.createInstance(String.valueOf(y0)), false));
        element.addAttribute(new MetadataAttribute("width", ProductData.ASCII.createInstance(String.valueOf(width)), false));
        element.addAttribute(new MetadataAttribute("height", ProductData.ASCII.createInstance(String.valueOf(height)), false));
        element.addAttribute(new MetadataAttribute("numComponents", ProductData.ASCII.createInstance(String.valueOf(getNumComponents())), false));
        for (ImageInfoComponent component : components) {
            element.addElement(component.toMetadataElement());
        }
        return element;
    }

    public class ImageInfoComponent {
        private int dx;
        private int dy;
        private int precision;
        private boolean isSigned;

        public ImageInfoComponent(int dx, int dy, int precision, boolean isSigned) {
            this.dx = dx;
            this.dy = dy;
            this.precision = precision;
            this.isSigned = isSigned;
        }

        public int getDx() {
            return dx;
        }

        public int getDy() {
            return dy;
        }

        public int getPrecision() {
            return precision;
        }

        public boolean isSigned() {
            return isSigned;
        }

        public MetadataElement toMetadataElement() {
            MetadataElement element = new MetadataElement("Component");
            element.addAttribute(new MetadataAttribute("dx", ProductData.ASCII.createInstance(String.valueOf(dx)), false));
            element.addAttribute(new MetadataAttribute("dy", ProductData.ASCII.createInstance(String.valueOf(dy)), false));
            element.addAttribute(new MetadataAttribute("precision", ProductData.ASCII.createInstance(String.valueOf(precision)), false));
            element.addAttribute(new MetadataAttribute("signed", ProductData.ASCII.createInstance(String.valueOf(isSigned)), false));
            return element;
        }
    }

}
