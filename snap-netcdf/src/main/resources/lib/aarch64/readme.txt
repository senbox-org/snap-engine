This directory contains native libraries for NetCDF Java interface for MacOS on ARM architectures (M1/M2 processors)
The following steps ahve been used to create this package:

- use homebrew on a M1/M2 mac to install libnetcdf
- copy all *.dylib from Homebrew/Cellar ... to this directory

The netcdf and hdf dylibs now contain *absolute* paths to the depending dylibs. The dependencies can be listed with
otool -L /path/to/dylib

The dependencies of the non-system dylibs need to be updated to relative paths:
install_name_tool -change "absolute/path/to/dylib" "@loader_path/dylib"

Now the code-signing of the dylib is invalidated. To regenerate:
codesign --force -s - /path/to/dylib

... and done.

