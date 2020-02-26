/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.jp2.reader.internal;

import org.esa.snap.lib.openjpeg.utils.CommandOutput;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kraftek on 7/15/2015.
 */
public class OpjExecutor {

    private Logger logger;
    private String exePath;
    private String lastError;
    private String lastOutput;

    public OpjExecutor(String executable) {
        logger = Logger.getLogger(OpjExecutor.class.getName());
        exePath = executable;
    }

    public int execute(Map<String, String> arguments) throws InterruptedException, IOException {
        lastError = null;
        lastOutput = null;
        List<String> args = new ArrayList<>();
        args.add(exePath);
        for (String key : arguments.keySet()) {
            args.add(key);
            args.add(arguments.get(key));
        }
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);

        CommandOutput commandOutput = OpenJpegUtils.runProcess(builder);
        lastOutput = commandOutput.getTextOutput();
        lastError = commandOutput.getErrorOutput();
        return commandOutput.getErrorCode();
    }

    public String getLastError() { return lastError; }
}
