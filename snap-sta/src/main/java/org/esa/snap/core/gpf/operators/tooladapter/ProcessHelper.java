/*
 * Copyright (C) 2018 CS ROMANIA
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
package org.esa.snap.core.gpf.operators.tooladapter;

import com.sun.jna.Pointer;
import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.operators.tooladapter.win.Kernel32;
import org.esa.snap.core.gpf.operators.tooladapter.win.NtDll;
import org.esa.snap.core.gpf.operators.tooladapter.win.Win32Api;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Cosmin Cara
 */
public final class ProcessHelper {

    private static final String WIN_KILL = "taskkill /PID %s /F /T";
    private static final String LINUX_KILL = "kill -9 %s";
    private static final String LINUX_SUSPEND = "kill -STOP %s";
    private static final String LINUX_RESUME = "kill -CONT %s";

    public static List<String> tokenizeCommands(String commandString) {
        return tokenize(commandString, "'");
    }

    public static List<String> tokenizeCommands(String commandString, Object...args) {
        return tokenize(commandString, "'", args);
    }

    private static List<String> tokenize(String commandString, String quoteChar, Object... replacements) {
        //'([^']*)'|(;)|([^;\s]+)
        String regex = quoteChar + "([^']*)" + quoteChar + "|(;)|([^;\\s]+)";
        final List<String> tokens = new ArrayList<>();
        String cmd = replacements != null && replacements.length > 0 ? String.format(commandString, replacements) : commandString;
        Matcher m = Pattern.compile(regex).matcher(String.format(cmd, replacements));
        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add(quoteChar + m.group(1) + quoteChar);
            } else if (m.group(2) != null) {
                tokens.add(m.group(2));
            } else if (m.group(3) != null) {
                tokens.add(m.group(3));
            }
        }
        return tokens;
    }

    public static int getPID(Process process) {
        int retVal = -1;
        if (ToolAdapterIO.getOsFamily().equals(OSFamily.windows.name())) {
            try {
                Field field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                long handle = field.getLong(process);
                Kernel32 kernel = Kernel32.INSTANCE;
                Win32Api.HANDLE h = new Win32Api.HANDLE();
                h.setPointer(Pointer.createConstant(handle));
                retVal = kernel.GetProcessId(h);
            } catch (Throwable ignored) { }
        } else if (ToolAdapterIO.getOsFamily().equals(OSFamily.linux.name())) {
            try {
                Field field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                retVal = field.getInt(process);
            } catch (Throwable ignored) { }
        } else {
            throw new UnsupportedOperationException();
        }
        return retVal;
    }

    public static void suspend(Process process) {
        if (ToolAdapterIO.getOsFamily().equals(OSFamily.windows.name())) {
            try {
                Field field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                long handle = field.getLong(process);
                NtDll ntDll = NtDll.INSTANCE;
                Win32Api.HANDLE h = new Win32Api.HANDLE();
                h.setPointer(Pointer.createConstant(handle));
                ntDll.NtSuspendProcess(h);
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        } else if (ToolAdapterIO.getOsFamily().equals(OSFamily.linux.name())) {
            try {
                Runtime.getRuntime().exec(String.format(LINUX_SUSPEND, getPID(process)));
            } catch (IOException ignored) { }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void resume(Process process) {
        if (ToolAdapterIO.getOsFamily().equals(OSFamily.windows.name())) {
            try {
                Field field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                long handle = field.getLong(process);
                NtDll ntDll = NtDll.INSTANCE;
                Win32Api.HANDLE h = new Win32Api.HANDLE();
                h.setPointer(Pointer.createConstant(handle));
                ntDll.NtResumeProcess(h);
            } catch (Throwable ignored) { }
        } else if (ToolAdapterIO.getOsFamily().equals(OSFamily.linux.name())) {
            try {
                Runtime.getRuntime().exec(String.format(LINUX_RESUME, getPID(process)));
            } catch (IOException ignored) { }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void terminate(Process process) {
        int pid = getPID(process);
        if (ToolAdapterIO.getOsFamily().equals(OSFamily.windows.name())) {
            try {
                Runtime.getRuntime().exec(String.format(WIN_KILL, pid));
            } catch (IOException ignored) { }
        } else if (ToolAdapterIO.getOsFamily().equals(OSFamily.linux.name())) {
            try {
                Runtime.getRuntime().exec(String.format(LINUX_KILL, pid));
            } catch (IOException ignored) { }
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
