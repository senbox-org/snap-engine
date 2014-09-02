@echo off

set S3TBX_HOME=${installer:sys.installationDir}

echo.
@echo Welcome to the S3TBX command-line interface!
@echo The following command-line tools are available:
@echo   gpt.bat            - General Graph Processing Tool
@echo   s3tbx-d.bat        - S3TBX application launcher for debugging
@echo   smos-gp-to-nc.bat  - Convert SMOS grid points to NetCDF
@echo   smos-gp-export.bat - Export SMOS grid points to EE or CSV format
@echo Typing the name of the tool will output its usage information.
echo.

cd "%S3TBX_HOME%\bin"

prompt $G$S
