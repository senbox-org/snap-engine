package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.tao.TAORemoteRepositoryProvider;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.util.Set;

/**
 * Created by jcoravu on 12/11/2019.
 */
public class RemoteRepositoriesManager {

    public static RemoteProductsRepositoryProvider[] getRemoteProductsRepositoryProviders() {
        ServiceRegistry<DataSource> registry = ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
        Set<DataSource> services = registry.getServices();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = new RemoteProductsRepositoryProvider[services.size()];
        int index = 0;
        for (DataSource dataSource : services) {
            remoteRepositoryProductProviders[index++] = new TAORemoteRepositoryProvider(dataSource);
        }
        return remoteRepositoryProductProviders;
    }
}
