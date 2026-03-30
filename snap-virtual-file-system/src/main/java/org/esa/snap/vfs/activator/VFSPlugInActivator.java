package org.esa.snap.vfs.activator;

import org.esa.snap.runtime.Activator;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin activator for VFS
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public class VFSPlugInActivator implements Activator {

    private static final Logger logger = Logger.getLogger(VFSPlugInActivator.class.getName());
    public static AtomicBoolean activated = new AtomicBoolean(false);

    /**
     * Creates the new plugin activator for VFS.
     */
    public VFSPlugInActivator() {
        //nothing to do
    }

    public static void activate() {
        if (!activated.getAndSet(true)) {
            try {
                Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
                List<VFSRemoteFileRepository> vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(configFile);
                VFS.getInstance().initRemoteInstalledProviders(vfsRepositories);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Unable to start the VFS Plugin. Reason: " + t.getMessage(), t);
            }
        }
    }

    /**
     * Starts the VFS plugin by initializing VFS providers with configurations stored in SNAP config files.
     */
    @Override
    public void start() {
        activate();
    }

    /**
     * Stops the VFS plugin
     */
    @Override
    public void stop() {
        // nothing to do
    }
}
