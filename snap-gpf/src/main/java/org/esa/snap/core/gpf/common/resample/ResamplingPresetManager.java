package org.esa.snap.core.gpf.common.resample;

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
 * Created by obarrile on 13/04/2019.
 */
public class ResamplingPresetManager {
    private final List<ResamplingPreset> _resamplingPresets;
    private final static Path resamplingPresetsDir;

    static {
        resamplingPresetsDir = SystemUtils.getAuxDataPath().resolve("resampling_presets");
        if (!Files.exists(resamplingPresetsDir)) {
            IOHelper.createDirectories(resamplingPresetsDir);
        }
    }

    private ResamplingPresetManager() {
        _resamplingPresets = new ArrayList<>();
        loadDefaultResamplingPresets();
    }

    public static Path getResamplingPresetsDir() {
        return resamplingPresetsDir;
    }

    public static ResamplingPresetManager getInstance() {
        return Holder.instance;
    }

    public void addResamplingPreset(ResamplingPreset preset) {
        if (preset != null && !_resamplingPresets.contains(preset)) {
            _resamplingPresets.add(preset);
        }
    }

    public void removeResamplingPreset(ResamplingPreset preset) {
        if (preset != null) {
            _resamplingPresets.remove(preset);
        }
    }

    public int getResamplingPresetCount() {
        return _resamplingPresets.size();
    }

    public ResamplingPreset getResamplingPreset(int i) {
        return _resamplingPresets.get(i);
    }

    public ResamplingPreset getResamplingPreset(String resamplingName) {
        for(ResamplingPreset resamplingPreset : _resamplingPresets) {
            if(resamplingPreset.getResamplingPresetName().equals(resamplingName)){
                return resamplingPreset;
            }
        }
        return null;
    }

    public ResamplingPreset[] getAllResamplingPresets() {
        return _resamplingPresets.toArray(new ResamplingPreset[_resamplingPresets.size()]);
    }

    /**
     * Loads all profiles from the BEAM default RGB profile location.
     */
    private void loadDefaultResamplingPresets() {
        if (!Files.exists(getResamplingPresetsDir())) {
            IOHelper.createDirectories(resamplingPresetsDir);
            SystemUtils.LOG.log(Level.INFO, "Directory for Resampling presets not found: " + getResamplingPresetsDir());
        }
        final Collection<Path> files = IOHelper.listFiles(getResamplingPresetsDir(), path -> path.getFileName().toString().endsWith(ResamplingPreset.FILENAME_EXTENSION) || path.getFileName().toString().endsWith(
                        ResamplingPreset.FILENAME_EXTENSION.toUpperCase()));
        if (files != null) {
            for (Path file : files) {
                try {
                    addResamplingPreset(ResamplingPreset.loadResamplingPreset(file));
                } catch (IOException e) {
                    SystemUtils.LOG.log(Level.SEVERE, "Failed to load Resampling preset from " + file, e);
                }
            }
        } else {
            SystemUtils.LOG.log(Level.INFO, "No Resampling presets found in " + getResamplingPresetsDir());
        }
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final ResamplingPresetManager instance = new ResamplingPresetManager();
    }
}
