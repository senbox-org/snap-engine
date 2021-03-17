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

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Iterator;

/**
 * A registry for markup language independent service provider instances of PersistenceSpi.
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i>
 *
 */
public class PersistenceSpiRegistry {
    private ServiceRegistry<PersistenceSpi> providers;

    private PersistenceSpiRegistry() {
        providers = ServiceRegistryManager.getInstance().getServiceRegistry(PersistenceSpi.class);
        ServiceLoader.loadServices(providers);
    }

    /**
     * Gets the singelton instance of this class.
     *
     * @return the instance
     */
    public static PersistenceSpiRegistry getInstance(){
        return PersistenceSpiRegistry.Holder.instance;
    }

    public void addPersistenceSpi(PersistenceSpi spi) {
        providers.addService(spi);
    }

    public Iterator<PersistenceSpi> getPersistenceSpis() {
        return providers.getServices().iterator();
    }

    public boolean isRegistered(PersistenceSpi spi) {
        return providers.getService(spi.getClass().getName()) != null;
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final PersistenceSpiRegistry instance = new PersistenceSpiRegistry();
    }
}
