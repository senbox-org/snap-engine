package org.esa.snap.core.dataio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface to be implemented by all simple metadata inspectors.
 *
 * @author Denisa Stefanescu
 */

public interface MetadataInspector {
    /**
     * Parses the metadata for the given product path
     */
    Metadata getMetadata(Path productPath) throws IOException;

    class Metadata {
        private Set<String> bandList = new HashSet<>();
        private Set<String> maskList = new HashSet<>();
        private boolean hasMasks;

        public Set<String> getBandList() {
            return bandList;
        }

        public void setBandList(Set<String> bandList) {
            this.bandList = bandList;
        }

        public Set<String> getMaskList() {
            return maskList;
        }

        public void setMaskList(Set<String> maskList) {
            this.maskList = maskList;
        }

        public boolean isHasMasks() {
            return hasMasks;
        }

        public void setHasMasks(boolean hasMasks) {
            this.hasMasks = hasMasks;
        }
    }
}
