@ECHO OFF

:: -======================-
:: User configurable values
:: -======================-

SET EE_TO_NETCDF_DIR=%cd%

::------------------------------------------------------------------
:: You can adjust the Java minimum and maximum heap space here.
:: Just change the Xms and Xmx options. Space is given in megabyte.
::    '-Xms64M' sets the minimum heap space to 64 megabytes
::    '-Xmx512M' sets the maximum heap space to 512 megabytes
::------------------------------------------------------------------
SET JAVA_OPTS=-Xms64M -Xmx1300M


:: -======================-
:: Other values
:: -======================-

IF "%JAVA_HOME%" == "" goto error

SET JAVAEXE=%JAVA_HOME%\bin\java.exe
SET MODULESDIR=%EE_TO_NETCDF_DIR%\..\modules
SET LIBDIR=%EE_TO_NETCDF_DIR%\..\lib
SET LAUNCHER_JAR=%EE_TO_NETCDF_DIR%\snap-launcher.jar
SET OLD_CLASSPATH=%CLASSPATH%

SET CLASSPATH=%LAUNCHER_JAR%;%LIBDIR%\*;%LIBDIR%;%MODULESDIR%\*;%MODULESDIR%

::------------------------------------------------------------------
:: You can adjust the Java minimum and maximum heap space here.
:: Just change the Xms and Xmx options. Space is given in megabyte.
::    '-Xms64M' sets the minimum heap space to 64 megabytes
::    '-Xmx512M' sets the maximum heap space to 512 megabytes
::------------------------------------------------------------------

CALL "%JAVAEXE%" %JAVA_OPTS% -classpath "%CLASSPATH%" org.esa.beam.smos.ee2netcdf.GPToNetCDFExporterTool %*

SET CLASSPATH=%OLD_CLASSPATH%
goto end


:error
echo ---------------------------------------------------------------------
echo No JDK found. Please be sure that JAVA_HOME points to valid JRE or JDK installation (where bin\java.exe is found)
echo ---------------------------------------------------------------------
echo JAVA_HOME = %JAVA_HOME%
pause

:end