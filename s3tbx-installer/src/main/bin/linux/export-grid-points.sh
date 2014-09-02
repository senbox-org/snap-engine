#! /bin/sh

export S3TBX_HOME=${installer:sys.installationDir}

if [ ! -d "${S3TBX_HOME}" ]
then
    PRGDIR=`dirname $0`
    export S3TBX_HOME=`cd "${PRGDIR}/.." ; pwd`
fi

if [ -z "${S3TBX_HOME}" ]; then
    echo
    echo Error:
    echo S3TBX_HOME does not exists in your environment. Please
    echo set the S3TBX_HOME variable in your environment to the
    echo location of your S3TBX installation.
    echo
    exit 2
fi

. "S3TBX_HOME/bin/detect_java.sh"

"${app_java_home}/bin/java" \
    -Xmx1024M \
    -Dceres.context=s3tbx \
    "-Ds3tbx.mainClass=org.esa.beam.smos.visat.export.GridPointExporter" \
    "-Ds3tbx.home=${S3TBX_HOME}" \
    -jar "${S3TBX_HOME}/bin/snap-launcher.jar" "$@"

exit 0