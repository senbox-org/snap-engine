package org.esa.snap.product.library.v2.activator;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.product.library.v2.database.H2DatabaseAccessor;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class ProductLibraryActivator implements Activator {

    private static final Logger logger = Logger.getLogger(ProductLibraryActivator.class.getName());

    public ProductLibraryActivator() {
    }

    @Override
    public void start() {
        try {
            ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
            ServiceRegistry<RemoteProductsRepositoryProvider> serviceRegistry = serviceRegistryManager.getServiceRegistry(RemoteProductsRepositoryProvider.class);
            ServiceLoader.loadServices(serviceRegistry);

            // load H2 driver
            Class.forName("org.h2.Driver");

            H2DatabaseAccessor.upgradeDatabase();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to initialize the database.", exception);
        }
    }

    @Override
    public void stop() {
        // do nothing
    }

    public static void main(String[] args) throws IOException {
        ProductLibraryActivator activator = new ProductLibraryActivator();
        activator.start();
    }
}