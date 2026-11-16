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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;


/**
 * A profile used for the creation of RGB images. The profile comprises the band arithmetic expressions
 * for the computation of red, green, blue and alpha (optional) channels of the resulting image.
 */
public class RGBImageProfileManager {

    private final List<RGBImageProfile> _profiles;
    private final static Path profilesDir;

    static {
        profilesDir = SystemUtils.getAuxDataPath().resolve("rgb_profiles");
        if (!Files.exists(profilesDir)) {
            IOHelper.createDirectories(profilesDir);
        }
    }

    private RGBImageProfileManager() {
        _profiles = new ArrayList<>();
        loadDefaultProfiles();
    }

    public static Path getProfilesDir() {
        return profilesDir;
    }

    public static RGBImageProfileManager getInstance() {
        return Holder.instance;
    }

    public void addProfile(RGBImageProfile profile) {
        if (profile != null && !_profiles.contains(profile)) {
            _profiles.add(profile);
        }
    }

    public void removeProfile(RGBImageProfile profile) {
        if (profile != null) {
            _profiles.remove(profile);
        }
    }

    public int getProfileCount() {
        return _profiles.size();
    }

    public RGBImageProfile getProfile(int i) {
        return _profiles.get(i);
    }

    public RGBImageProfile[] getAllProfiles() {
        return _profiles.toArray(new RGBImageProfile[_profiles.size()]);
    }

    /**
     * Loads all profiles from the BEAM default RGB profile location.
     */
    private void loadDefaultProfiles() {
        if (!Files.exists(getProfilesDir())) {
            IOHelper.createDirectories(profilesDir);
            SystemUtils.LOG.log(Level.INFO, "Directory for RGB-image profiles not found: " + getProfilesDir());
        }
        final Collection<Path> files = IOHelper.listFiles(getProfilesDir(), path -> {
            final String name = path.getFileName().toString();
            return name.endsWith(RGBImageProfile.FILENAME_EXTENSION) || name.endsWith(
                    RGBImageProfile.FILENAME_EXTENSION.toUpperCase());
        });
        if (files != null) {
            for (Path file : files) {
                try {
                    addProfile(RGBImageProfile.loadProfile(file));
                } catch (IOException e) {
                    SystemUtils.LOG.log(Level.SEVERE, "Failed to load RGB-image profile from " + file, e);
                }
            }
        } else {
            SystemUtils.LOG.log(Level.INFO, "No RGB-image profiles found in " + getProfilesDir());
        }
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final RGBImageProfileManager instance = new RGBImageProfileManager();
    }
}
