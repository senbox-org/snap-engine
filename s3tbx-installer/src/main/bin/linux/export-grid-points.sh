#! /bin/sh

export SNAP_HOME=${installer:sys.installationDir}

if [ ! -d "${SNAP_HOME}" ]
then
    PRGDIR=`dirname $0`
    export SNAP_HOME=`cd "${PRGDIR}/.." ; pwd`
fi

if [ -z "${SNAP_HOME}" ]; then
    echo
    echo Error:
    echo SNAP_HOME does not exists in your environment. Please
    echo set the SNAP_HOME variable in your environment to the
    echo location of your SNAP installation.
    echo
    exit 2
fi

. "SNAP_HOME/bin/detect_java.sh"

"${app_java_home}/bin/java" \
    -Xmx1024M \
    -Dceres.context=s3tbx \
    "-Ds3tbx.mainClass=org.esa.beam.smos.visat.export.GridPointExporter" \
    "-Ds3tbx.home=${SNAP_HOME}" \
    -jar "${SNAP_HOME}/bin/snap-launcher.jar" "$@"

exit 0