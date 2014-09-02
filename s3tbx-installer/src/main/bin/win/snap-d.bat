@echo off

set SNAP_HOME=${installer:sys.installationDir}

"%SNAP_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=s3tbx ^
    -Ds3tbx.debug=true ^
    "-Ds3tbx.home=%SNAP_HOME%" ^
    -jar "%SNAP_HOME%\bin\snap-launcher.jar" -d %*

exit /B %ERRORLEVEL%
