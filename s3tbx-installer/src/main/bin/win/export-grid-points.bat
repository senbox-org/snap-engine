@echo off

set SNAP_HOME=${installer:sys.installationDir}

"%SNAP_HOME%\jre\bin\java.exe" ^
    -Xmx1024M ^
    -Dceres.context=s3tbx ^
    "-Ds3tbx.mainClass=org.esa.beam.smos.visat.export.GridPointExporter" ^
    "-Ds3tbx.home=%SNAP_HOME%" ^
    -jar "%SNAP_HOME%\bin\snap-launcher.jar" %*

exit /B 0
