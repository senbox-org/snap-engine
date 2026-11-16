package org.esa.snap.binning.operator;

import com.bc.ceres.core.io.EnhancedNIORandomAccessFile;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Norman
 */
public class MappedFile {

    private final Path file;
    private final FileChannel.MapMode mapMode;
    private EnhancedNIORandomAccessFile raf;
    private MappedByteBuffer buffer;

    private MappedFile(Path file, FileChannel.MapMode mapMode) throws IOException {
        this.file = file;
        this.mapMode = mapMode;
    }

    public static MappedFile open(Path file, FileChannel.MapMode mapMode, long position, long size) throws IOException {
        MappedFile mappedFile = new MappedFile(file, mapMode);
        mappedFile.map(position, size);
        return mappedFile;
    }

    public static MappedFile open(Path file, long size) throws IOException {
        return open(file, FileChannel.MapMode.READ_WRITE, 0, size);
    }

    public static MappedFile open(Path file) throws IOException {
        return open(file, FileChannel.MapMode.READ_ONLY, 0, Files.size(file));
    }

    public Path getFile() {
        return file;
    }

    public MappedByteBuffer getBuffer() {
        return buffer;
    }

    public MappedByteBuffer remap(long position, long size) throws IOException {
        close();
        map(position, size);
        return getBuffer();
    }

    public void close() throws IOException {
        this.raf.close();
    }

    private void map(long position, long size) throws IOException {
        final OpenOption[] modes;
        if (mapMode == FileChannel.MapMode.READ_ONLY) {
            modes = new OpenOption[]{StandardOpenOption.READ};
        } else {
            modes = new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE};
        }
        raf = new EnhancedNIORandomAccessFile(file, modes);
        buffer = raf.withChannel(channel -> channel.map(mapMode, position, size));
    }
}
