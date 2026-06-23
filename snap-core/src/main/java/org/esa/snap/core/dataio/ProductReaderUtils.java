package org.esa.snap.core.dataio;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProductReaderUtils {

    public static final Class<?>[] IO_TYPES = new Class[]{
            Path.class,
            File.class,
            String.class
    };
    public static final OutputConverter[] IO_CONVERTERS = new OutputConverter[]{
            output -> (Path) output,
            output -> ((File) output).toPath(),
            output -> Paths.get((String) output)
    };

    public static Path convertToPath(final Object object) {
        for (int i = 0; i < IO_TYPES.length; i++) {
            if (IO_TYPES[i].isInstance(object)) {
                return IO_CONVERTERS[i].convertOutput(object);
            }
        }
        return null;
    }

    private interface OutputConverter {

        Path convertOutput(Object output);
    }
}
