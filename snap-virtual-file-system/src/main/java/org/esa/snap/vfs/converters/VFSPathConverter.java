package org.esa.snap.vfs.converters;

import com.bc.ceres.binding.PathConverter;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.esa.snap.vfs.VFS;

/**
 * Implements a {@code PathConverter} for VFS.
 *
 * @author Alvaro Huarte
 */
public class VFSPathConverter implements PathConverter {

    @Override
    public boolean matches(String text) {
        Path path = VFS.getInstance().get(text);
        return path != null && !path.getFileSystem().getClass().equals(FileSystems.getDefault().getClass());
    }

    @Override
    public Path parse(String text) {
        return VFS.getInstance().get(text);
    }
}
