@echo off

set S3TBX_HOME=${installer:sys.installationDir}

"%S3TBX_HOME%\jre\bin\java.exe" ^
    -Xmx1024M ^
    -Dceres.context=s3tbx ^
    "-Ds3tbx.mainClass=org.esa.beam.smos.ee2netcdf.GPToNetCDFExporterTool" ^
    "-Ds3tbx.home=%S3TBX_HOME%" ^
    -jar "%S3TBX_HOME%\bin\snap-launcher.jar" %*

exit /B 0