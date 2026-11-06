package org.esa.snap.runtime;

/**
 * A service allowing a module to execute code when the SNAP engine is started and stopped. Typical use cases
 * include installation/deinstallation of a module's auxiliary data or registration/deregistration of secondary
 * services.
 * <p/>
 * Engine module activators are registered using the standard JAR service provider interface, i.e. a module must
 * provide a resource file {@code META-INF/services/org.esa.snap.runtime.Activator} whose lines are fully qualified
 * names of module classes that implement the {@code Activator} interface.
 *
 * @author Norman Fomferra
 */
public interface Activator {
    /**
     * Called when the SNAP engine is started.
     */
    void start();

    /**
     * Called when the SNAP engine has stopped.
     */
    void stop();

    /**
     * The start level of the activator which defaults to zero.
     * Activators with a lower level are started before and stopped after those with a higher level.
     *
     * @return The start level of the activator.
     */
    default int getStartLevel() {
        return 0;
    }

    /**
     * Whether this activator may be started on a background thread after
     * {@link Engine#start(boolean, java.util.concurrent.Executor)} has returned, instead of synchronously during the
     * engine start. Deferral only happens if the engine is started with a non-null executor, which SNAP Desktop does
     * to keep such work out of its startup.
     * <p>
     * Only return {@code true} if nothing relies on the activator having run when the engine start returns, i.e. all
     * code depending on its work initialises it on demand in a thread-safe way that waits for a running
     * initialisation to complete. Deferred activators are started in start-level order among themselves, but after
     * all non-deferred activators.
     *
     * @return {@code true} if the activator may be started in the background, defaults to {@code false}
     */
    default boolean isDeferrable() {
        return false;
    }
}
