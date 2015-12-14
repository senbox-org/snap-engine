/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.meris.l2auxdata;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.esa.snap.core.datamodel.Product;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class L2AuxDataProvider {
    private static L2AuxDataProvider instance;
    private org.esa.s3tbx.meris.l2auxdata.DpmConfig dpmConfig;
    private final Map<Product, L2AuxData> map;
    
    public static synchronized L2AuxDataProvider getInstance() {
        if (instance == null) {
            instance = new L2AuxDataProvider();
        }
        return instance;
    }
    
    private L2AuxDataProvider() {
        map = new WeakHashMap<Product, L2AuxData>();
    }
    
    public synchronized L2AuxData getAuxdata(Product product) throws L2AuxDataException {
        getDpmConfig();
        L2AuxData auxData = map.get(product);
        if (auxData == null) {
            auxData = loadAuxdata(product);
            map.put(product, auxData);
        }
        return auxData;
    }
    
    public synchronized DpmConfig getDpmConfig() throws L2AuxDataException {
        if (dpmConfig == null ) {
            loadDpmConfig();
        }
        return dpmConfig;
    }

    private L2AuxData loadAuxdata(Product product) throws L2AuxDataException {
        L2AuxData auxData;
        try {
            auxData = new L2AuxData(dpmConfig, product);
        } catch (IOException e) {
            throw new L2AuxDataException(e.getMessage(), e);
        }
        return auxData;
    }
    
    private void loadDpmConfig() throws L2AuxDataException {
        dpmConfig = new DpmConfig();
    }
}
