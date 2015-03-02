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

package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.jdom.Element;

public class PodGeoCodingPersistable implements DimapPersistable {

    static final String POD_GEO_CODING_TAG = "PodGeoCoding";

    @Override
    public Object createObjectFromXml(Element element, Product product) {
        Element podGeoCodingElement = element.getChild(POD_GEO_CODING_TAG);

        String tpgNameLat = podGeoCodingElement.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT);
        String tpgNameLon = podGeoCodingElement.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON);

        if (tpgNameLat != null && tpgNameLon != null) {
            TiePointGrid tiePointGridLat = product.getTiePointGrid(tpgNameLat);
            TiePointGrid tiePointGridLon = product.getTiePointGrid(tpgNameLon);
            if (tiePointGridLat != null && tiePointGridLon != null) {
                if (tiePointGridLat.hasRasterData() && tiePointGridLon.hasRasterData()) {
                    return new PodGeoCoding(tiePointGridLat, tiePointGridLon);
                }
            }
        }
        return null;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        PodGeoCoding podGeoCoding = (PodGeoCoding) object;
        final Element podGeoCodingElement = new Element(POD_GEO_CODING_TAG);

        final Element tpgLatElement = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT);
        tpgLatElement.setText(podGeoCoding.getLatGrid().getName());
        podGeoCodingElement.addContent(tpgLatElement);

        final Element tpgLonElement = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON);
        tpgLonElement.setText(podGeoCoding.getLonGrid().getName());
        podGeoCodingElement.addContent(tpgLonElement);

        return podGeoCodingElement;
    }
}
