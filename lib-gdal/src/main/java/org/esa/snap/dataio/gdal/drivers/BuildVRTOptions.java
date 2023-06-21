package org.esa.snap.dataio.gdal.drivers;

import java.util.Vector;

/**
 * GDAL BuildVRTOptions JNI driver class
 *
 * @author Adrian Drăghici
 */
public class BuildVRTOptions {

    /**
     * The name of JNI GDAL BuildVRTOptions class
     */
    private static final String CLASS_NAME = "org.gdal.gdal.BuildVRTOptions";

    private final Object jniBuildVRTOptionsInstance;

    /**
     * Creates new instance for this driver
     *
     * @param jniBuildVRTOptionsInstance the JNI GDAL BuildVRTOptions class instance
     */
    public BuildVRTOptions(Object jniBuildVRTOptionsInstance) {
        this.jniBuildVRTOptionsInstance = jniBuildVRTOptionsInstance;
    }

    /**
     * Creates new GDAL BuildVRTOptions class instance
     */
    public BuildVRTOptions(Vector options) {
        this.jniBuildVRTOptionsInstance = GDALReflection.fetchGDALLibraryClassInstance(CLASS_NAME, new Class[]{Vector.class}, new Object[]{options});
    }

    public Object getJniBuildVRTOptionsInstance() {
        return jniBuildVRTOptionsInstance;
    }

}
