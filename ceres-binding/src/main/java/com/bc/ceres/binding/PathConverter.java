package com.bc.ceres.binding;

import java.nio.file.Path;

/**
 * A {@code PathConverter} provides a strategy to convert from plain text
 * to a {@code Path} object instance.
 *
 * @author Alvaro Huarte
 */
public interface PathConverter {
    /**
     * Tells whether or not this instance supports the given textual 
     * representation of a {@code Path}.
     * 
     * @param text The textual representation of the Path.
     * @return True if this instance supports the textual representation.
     */
    boolean matches(String text);

    /**
     * Converts a Path from its plain text representation to 
     * a {@code Path} object instance.
     *
     * @param text The textual representation of the Path.
     * @return The converted Path.
     */
    Path parse(String text);
}
