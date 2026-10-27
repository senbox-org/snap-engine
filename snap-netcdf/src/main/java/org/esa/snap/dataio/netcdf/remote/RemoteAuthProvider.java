package org.esa.snap.dataio.netcdf.remote;

import java.io.IOException;
import java.net.URI;

/**
 * Supplies the {@code Authorization} header for remote NetCDF/HDF5 files read over HTTP(S).
 * <p>
 * Register an implementation with {@link RemoteFileAccess#registerAuthProvider(RemoteAuthProvider)}.
 * The header is sent only to hosts for which {@link #handles(URI)} returns {@code true}, so credentials
 * are never forwarded to the storage or CDN host a server redirects to.
 */
public interface RemoteAuthProvider {

    /**
     * @return true if this provider has credentials for the host of the given URI
     */
    boolean handles(URI uri);

    /**
     * @return the full header value, e.g. {@code "Bearer <token>"}, or null to send no header
     */
    String getAuthorizationHeader(URI uri) throws IOException;

    /**
     * Called after the server rejected the header (HTTP 401), so cached credentials can be refreshed.
     */
    default void invalidate(URI uri) {
    }
}
