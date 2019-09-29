package org.esa.snap.remote.execution.file.system;

import java.io.IOException;

/**
 * Created by jcoravu on 13/3/2019.
 */
public abstract class UnixLocalMachineFileSystem implements ILocalMachineFileSystem {

    protected UnixLocalMachineFileSystem() {
    }

    @Override
    public char getFileSeparator() {
        return '/';
    }

    @Override
    public String normalizeFileSeparator(String path) {
        return normalizeUnixPath(path);
    }

    @Override
    public boolean pathStartsWith(String path, String prefix) {
        return path.startsWith(prefix);
    }

    @Override
    public String findPhysicalSharedFolderPath(String shareNameToFind, String localPassword) throws IOException {
        return null;
    }

    public static String normalizeUnixPath(String path) {
        return path.replace('\\', '/');
    }
}
