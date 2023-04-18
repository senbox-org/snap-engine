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

/**
 * Helper class to terminate/suspend/resume processes on Windows, Linux and MacOS.
 *
 * @author Cosmin Cara
 */
public final class ProcessHelper {

    private static final String WIN_KILL = "taskkill /PID %s /F /T";
    private static final String LINUX_KILL = "kill -9 -- -%d";
    private static final String LINUX_SUSPEND = "kill -STOP %d";
    private static final String LINUX_RESUME = "kill -CONT %d";

    /**
     * Returns the OS-specific PID of the given process
     */
    public static int getPID(Process process) {
        int retVal = -1;
        final OSFamily os = OSFamily.valueOf(ToolAdapterIO.getOsFamily());
        switch (os) {
            case windows:
                try {
                    final Win32Api.HANDLE h = getWinProcessHandle(process);
                    retVal = Kernel32.INSTANCE.GetProcessId(h);
                } catch (Throwable ignored) { }
                break;
            case linux:
            case macosx:
                try {
                    Field field = process.getClass().getDeclaredField("pid");
                    field.setAccessible(true);
                    retVal = field.getInt(process);
                } catch (Throwable ignored) { }
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return retVal;
    }

    /**
     * Suspends (pauses) the given process.
     */
    public static void suspend(Process process) {
        final OSFamily os = OSFamily.valueOf(ToolAdapterIO.getOsFamily());
        switch (os) {
            case windows:
                try {
                    NtDll.INSTANCE.NtSuspendProcess(getWinProcessHandle(process));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                break;
            case linux:
            case macosx:
                try {
                    Runtime.getRuntime().exec(String.format(LINUX_SUSPEND, getPID(process)));
                } catch (IOException ignored) { }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Resumes the given process.
     */
    public static void resume(Process process) {
        final OSFamily os = OSFamily.valueOf(ToolAdapterIO.getOsFamily());
        switch (os) {
            case windows:
                try {
                    NtDll.INSTANCE.NtResumeProcess(getWinProcessHandle(process));
                } catch (Throwable ignored) {
                }
                break;
            case linux:
            case macosx:
                try {
                    Runtime.getRuntime().exec(String.format(LINUX_RESUME, getPID(process)));
                } catch (IOException ignored) {
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Terminates the given process and its subprocesses (if any).
     */
    public static void terminate(Process process) {
        int pid = getPID(process);
        final OSFamily os = OSFamily.valueOf(ToolAdapterIO.getOsFamily());
        switch (os) {
            case windows:
                try {
                    Runtime.getRuntime().exec(String.format(WIN_KILL, pid));
                } catch (IOException ignored) { }
                break;
            case linux:
            case macosx:
                try {
                    Runtime.getRuntime().exec(String.format(LINUX_KILL, pid));
                } catch (IOException ignored) { }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static Win32Api.HANDLE getWinProcessHandle(Process process) throws NoSuchFieldException, IllegalAccessException {
        final Field field = process.getClass().getDeclaredField("handle");
        field.setAccessible(true);
        final long handle = field.getLong(process);
        final Win32Api.HANDLE h = new Win32Api.HANDLE();
        h.setPointer(Pointer.createConstant(handle));
        return h;
    }
}
