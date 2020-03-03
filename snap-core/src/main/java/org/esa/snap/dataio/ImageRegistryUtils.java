package org.esa.snap.dataio;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.util.Iterator;

/**
 * Created by jcoravu on 28/11/2019.
 */
public class ImageRegistryUtils {

    private ImageRegistryUtils() {
    }

    public static void deregisterImageInputStreamSpi(ImageInputStreamSpi imageInputStreamSpi) {
        if (imageInputStreamSpi == null) {
            throw new NullPointerException("The image spi is null.");
        }
        IIORegistry.getDefaultInstance().deregisterServiceProvider(imageInputStreamSpi);
    }

    public static FileImageInputStreamSpi registerImageInputStreamSpi() {
        FileImageInputStreamSpi imageInputStreamSpi = null;
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        if (defaultInstance.getServiceProviderByClass(FileImageInputStreamSpi.class) == null) {
            // register only if not already registered
            ImageInputStreamSpi toUnorder = null;
            Iterator<ImageInputStreamSpi> serviceProviders = defaultInstance.getServiceProviders(ImageInputStreamSpi.class, true);
            while (serviceProviders.hasNext()) {
                ImageInputStreamSpi current = serviceProviders.next();
                if (current.getInputClass() == File.class) {
                    toUnorder = current;
                    break;
                }
            }
            imageInputStreamSpi = new FileImageInputStreamSpi();
            defaultInstance.registerServiceProvider(imageInputStreamSpi);
            if (toUnorder != null) {
                // Make the custom Spi to be the first one to be used.
                defaultInstance.setOrdering(ImageInputStreamSpi.class, imageInputStreamSpi, toUnorder);
            }
        }
        return imageInputStreamSpi;
    }
}
