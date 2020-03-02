package org.esa.snap.core.image;

import javax.media.jai.PlanarImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple class to dispose (close) all TileOpImages.
 * It will call PlanarImage.dispose() on all registered items.
 *
 * @author Cosmin Cara
 */
public class TileImageDisposer {

    private final List<PlanarImage> tileOpImages;

    public TileImageDisposer() {
        this.tileOpImages = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Adds an image to be handled by this instance.
     *
     * @param image The image to be (later) disposed.
     */
    public void registerForDisposal(PlanarImage image) {
        this.tileOpImages.add(image);
    }

    /**
     * Disposes all registered images.
     */
    public void disposeAll() {
        int index = -1;
        while ((index = this.tileOpImages.size() - 1) >= 0) {
            PlanarImage image = this.tileOpImages.remove(index);
            if (image != null) {
                image.dispose();
                image = null;
            }
        }
    }
}
