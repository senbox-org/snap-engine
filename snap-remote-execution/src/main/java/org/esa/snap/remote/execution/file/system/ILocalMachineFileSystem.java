package org.esa.snap.remote.execution.file.system;

import java.io.IOException;

/**
 * Created by jcoravu on 13/3/2019.
 */
public interface ILocalMachineFileSystem {

    public char getFileSeparator();

    public String normalizeFileSeparator(String path);

    public boolean pathStartsWith(String path, String prefix);

    public String findPhysicalSharedFolderPath(String shareNameToFind, String localPassword) throws IOException;
}
