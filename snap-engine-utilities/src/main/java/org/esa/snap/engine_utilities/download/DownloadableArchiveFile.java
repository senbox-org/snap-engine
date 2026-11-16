/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.download;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Created by lveci on 11/02/2015.
 */
public class DownloadableArchiveFile implements Path, DownloadableFile {

    private final Path file;

    public DownloadableArchiveFile(final Path file) {
        this.file = file;
    }

    public void dispose() {
    }

    @Override
    public @NonNull FileSystem getFileSystem() {
        return file.getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    @Override
    public Path getRoot() {
        return file.getRoot();
    }

    @Override
    public Path getFileName() {
        return file.getFileName();
    }

    @Override
    public Path getParent() {
        return file.getParent();
    }

    @Override
    public int getNameCount() {
        return file.getNameCount();
    }

    @Override
    public @NonNull Path getName(int index) {
        return file.getName(index);
    }

    @Override
    public @NonNull Path subpath(int beginIndex, int endIndex) {
        return file.subpath(beginIndex, endIndex);
    }

    @Override
    public boolean startsWith(@NonNull Path other) {
        return file.startsWith(other);
    }

    @Override
    public boolean endsWith(@NonNull Path other) {
        return file.endsWith(other);
    }

    @Override
    public @NonNull Path normalize() {
        return file.normalize();
    }

    @Override
    public @NonNull Path resolve(@NonNull Path other) {
        return file.resolve(other);
    }

    @Override
    public @NonNull Path relativize(@NonNull Path other) {
        return file.relativize(other);
    }

    @Override
    public @NonNull URI toUri() {
        return file.toUri();
    }

    @Override
    public @NonNull Path toAbsolutePath() {
        return file.toAbsolutePath();
    }

    @Override
    public @NonNull Path toRealPath(@NonNull @NotNull LinkOption... options) throws IOException {
        return file.toRealPath(options);
    }

    @Override
    public @NonNull WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return file.register(watcher, events, modifiers);
    }

    @Override
    public int compareTo(@NonNull Path other) {
        return file.compareTo(other);
    }

    @Override
    public boolean equals(Object other) {
        return file.equals(other);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return file.toString();
    }
}
