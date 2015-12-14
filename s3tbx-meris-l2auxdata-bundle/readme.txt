This module provides the auxiliary data for the BRR, SDR and ICOL processors.
It originates from the MERIS L2 processor
In order to deploy this module

1. Extract the auxiliary file into the 'src/main/resources' directory.
   The auxiliary data can be found on our internal server at
   'fs1\projects\ongoing\icol\meris-l2auxdata'

2. Run 'mvn deploy' from the module directory:

   mvn deploy

   Define an alternative repository, when the repository specified in
   the <distributuionManagement> is not available:

   -DaltDeploymentRepository=bc::default::file:///Volumes/fs1/pub/webservers/www.brockmann-consult.de/mvn/os

   and then 'trigger' synchronisation of the web server.