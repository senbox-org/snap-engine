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
SET MODULESDIR=%EE_TO_NETCDF_DIR%\..\modules
SET LIBDIR=%EE_TO_NETCDF_DIR%\..\lib
SET LAUNCHER_JAR=%EE_TO_NETCDF_DIR%\snap-launcher.jar
SET OLD_CLASSPATH=%CLASSPATH%

SET CLASSPATH=%LAUNCHER_JAR%;%LIBDIR%\*;%LIBDIR%;%MODULESDIR%\*;%MODULESDIR%

"$JAVAEXE" "$JAVA_OPTS" -classpath "$CLASSPATH" org.esa.beam.smos.ee2netcdf.GPToNetCDFExporterTool "$@"

export CLASSPATH="$OLD_CLASSPATH"
