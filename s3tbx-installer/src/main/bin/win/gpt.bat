@echo off

set SNAP_HOME=${installer:sys.installationDir}

"%SNAP_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=s3tbx ^
    "-Ds3tbx.mainClass=org.esa.beam.framework.gpf.main.GPT" ^
    "-Ds3tbx.home=%SNAP_HOME%" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%SNAP_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%SNAP_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf5.dll" ^
    -jar "%SNAP_HOME%\bin\snap-launcher.jar" %*

exit /B %ERRORLEVEL%
