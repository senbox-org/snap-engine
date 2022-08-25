package org.esa.snap.product.library.v2.activator;

import org.esa.snap.product.library.v2.database.DataAccess;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The activator class used to initialize the H2 local database of the Product Library Tool.
 *
 * Created by jcoravu on 3/9/2019.
 *
 * Updated August 2022:
 * Improve SNAP startup time by initialising Product Library on first usage
 * (therefore remove the Activator way of initializing modules during SNAP startup)
 */
public class ProductLibraryActivator {

    private static final Logger logger = Logger.getLogger(ProductLibraryActivator.class.getName());

    public static void start() {
        try {
            // load H2 driver
            Class.forName("org.h2.Driver");
            DataAccess.initialize();
            DataAccess.upgradeDatabase();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to initialize the database.", exception);
        }
    }
}