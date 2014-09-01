#!/bin/bash

# -======================-
# User configurable values
# -======================-

export EE_TO_NETCDF_DIR="`pwd`"

#------------------------------------------------------------------
# You can adjust the Java minimum and maximum heap space here.
# Just change the Xms and Xmx options. Space is given in megabyte.
#    '-Xms64M' sets the minimum heap space to 64 megabytes
#    '-Xmx512M' sets the maximum heap space to 512 megabytes
#------------------------------------------------------------------
export JAVA_OPTS="-Xmx${installer:maxHeapSize}"




# -======================-
# Other values
# -======================-

export JAVAEXE="$JAVA_HOME"/bin/java
export LIBDIR="$EE_TO_NETCDF_DIR"/lib
export OLD_CLASSPATH="$CLASSPATH"
CLASSPATH="$LIBDIR/*:$LIBDIR"

"$JAVAEXE" "$JAVA_OPTS" -classpath "$CLASSPATH" org.esa.beam.smos.ee2netcdf.GPToNetCDFExporterTool "$@"

export CLASSPATH="$OLD_CLASSPATH"
