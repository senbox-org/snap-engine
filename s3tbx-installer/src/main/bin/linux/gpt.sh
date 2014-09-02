#! /bin/sh

export SNAP_HOME=${installer:sys.installationDir}

if [ -z "$SNAP_HOME" ]; then
    echo
    echo Error: SNAP_HOME not found in your environment.
    echo Please set the SNAP_HOME variable in your environment to match the
    echo location of the SNAP installation
    echo
    exit 2
fi

. "$SNAP_HOME/bin/detect_java.sh"

"$app_java_home/bin/java" \
    -Xmx${installer:maxHeapSize} \
    -Dceres.context=s3tbx \
    "-Ds3tbx.mainClass=org.esa.beam.framework.gpf.main.GPT" \
    "-Ds3tbx.home=$SNAP_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$SNAP_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$SNAP_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf5.so" \
    -jar "$SNAP_HOME/bin/snap-launcher.jar" "$@"

exit $?