/*
 * $Id: DpmConfigException.java,v 1.1 2007/03/27 12:51:41 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.l2auxdata;

public class L2AuxDataException extends Exception {

    static final String MSG_PATTERN = "Could not load MERIS L2 auxiliary data: %1$s";

    /**
     * Constructs a new exception with the specified detail message.  The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    public L2AuxDataException(String message) {
        super(String.format(MSG_PATTERN, message));
    }

    /**
     * Constructs a new exception with the specified detail message and cause.  <p>Note that the detail message
     * associated with <code>cause</code> is <i>not</i> automatically incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
     *                <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @since 1.4
     */
    public L2AuxDataException(String message, Throwable cause) {
        super(String.format(MSG_PATTERN, message), cause);
    }
}
