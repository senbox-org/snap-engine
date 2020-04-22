/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
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

package org.esa.snap.lib.openjpeg.utils;

/**
 *  @author Oscar
 */
public class CommandOutput {
    private int errorCode;
    private String textOutput;
    private String errorOutput;

    public CommandOutput(int errorCode, String textOutput, String errorOutput) {
        this.errorCode = errorCode;
        this.textOutput = textOutput;
        this.errorOutput = errorOutput;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getTextOutput() {
        return textOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }
}
