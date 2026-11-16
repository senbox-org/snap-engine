/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An editor for parameters of type {@link Path}. This editor is composed of a text
 * field and a button labeled "...", which opens a file chooser.
 */
public class FileEditor extends TextFieldXEditor {

    private static final String _FILE_APPROVE_BUTTON_TEXT = "Select File"; /*I18N*/
    private static final String _DIR_APPROVE_BUTTON_TEXT = "Select Directory";   /*I18N*/
    private static final String _DEFAULT_APPROVE_BUTTON_TEXT = "Select";          /*I18N*/

    public FileEditor(Parameter parameter) {
        super(parameter);
        updateLastDir();
    }

    @Override
    protected void invokeXEditor() {
        Path lastDir = getLastDir();
        Path file = getParameterFileValue();
        JFileChooser fileChooser;
        int fsm = getParameter().getProperties().getFileSelectionMode();
        if (fsm == ParamProperties.FSM_DIRECTORIES_ONLY) {
            if (file == null) {
                file = lastDir;
            }
            fileChooser = createDirChooser(file);
        } else {
            fileChooser = createFileChooser(lastDir);
        }
        fileChooser.setFileSelectionMode(fsm);
        if (file != null && Files.exists(file)) {
            fileChooser.setSelectedFile(file.toFile());
        }

        fileChooser.setDialogTitle(getXEditorTitle());
        final FileFilter[] choosable = getParameter().getProperties().getChoosableFileFilters();
        if (choosable != null) {
            for (int i = choosable.length - 1; i > -1; i--) {
                FileFilter filter = choosable[i];
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        final FileFilter current = getParameter().getProperties().getCurrentFileFilter();
        if (current != null) {
            fileChooser.setFileFilter(current);
        }
        int option;
        if (fsm == ParamProperties.FSM_FILES_ONLY) {
            option = fileChooser.showDialog(getEditorComponent(), _FILE_APPROVE_BUTTON_TEXT);
        } else if (fsm == ParamProperties.FSM_DIRECTORIES_ONLY) {
            option = fileChooser.showDialog(getEditorComponent(), _DIR_APPROVE_BUTTON_TEXT);
        } else {
            option = fileChooser.showDialog(getEditorComponent(), _DEFAULT_APPROVE_BUTTON_TEXT);
        }
        setLastDir(fileChooser.getCurrentDirectory().toPath());
        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile() == null ? null : fileChooser.getSelectedFile().toPath();
            if (file != null) {
                setParameterFileValue(file);
            }
        }
    }

    private Path getParameterFileValue() {
        return (Path) getParameter().getValue();
    }

    private void setParameterFileValue(Path file) {
        getParameter().setValue(file, null);
        updateLastDir();
    }

    private void updateLastDir() {
        Path file = (Path) getParameter().getValue();
        if (file != null) {
            Path parentFile = file.getParent();
            if (parentFile != null) {
                setLastDir(parentFile);
            }
        }
    }

    private Path getLastDir() {
        if (getParameter().getValue() instanceof Path file) {
            final Path parentDir = file.getParent();
            if (parentDir != null && Files.isDirectory(parentDir)) {
                return parentDir;
            }
        }
        return (Path) getParameter().getProperties().getPropertyValue(ParamProperties.LAST_DIR_KEY,
                                                                      SystemUtils.getApplicationHomeDir());
    }

    private void setLastDir(final Path lastDir) {
        getParameter().getProperties().setPropertyValue(ParamProperties.LAST_DIR_KEY, lastDir);
    }

    public JFileChooser createFileChooser(Path currentDirectory) {
        JFileChooser fileChooser = new JFileChooser(currentDirectory.toFile());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }

    public JFileChooser createDirChooser(Path currentDirectory) {
        JFileChooser dirChooser = new JFileChooser(currentDirectory.toFile());
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return dirChooser;
    }

}
