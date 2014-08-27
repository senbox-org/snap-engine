import sys
import sysconfig
import os
import os.path
import platform
import argparse
import zipfile
import subprocess
import logging


def _configure_beampy(java_home=None,
                      req_arch=None,
                      req_java=False,
                      req_py=False,
                      force=False):

    if req_arch:
        req_arch = req_arch.lower()
        act_arch = platform.machine().lower()
        if req_arch != act_arch:
            logging.warning("architecture requirement possibly not met: Python is " + act_arch + " but JVM requires " + req_arch)
        is64 = sys.maxsize > 2 ** 31 - 1
        if is64 and not req_arch in ('amd64', 'ia64', 'x64', 'x86_64'):
            logging.warning("architecture requirement possibly not met: Python is 64 bit but JVM requires " + req_arch)

    beampy_dir = os.path.dirname(os.path.abspath(__file__))

    #
    # Extract a matching jpy binary distribution from ../lib/jpy.<platform>-<python-version>.zip
    #

    jpy_distr_name = 'jpy.' + sysconfig.get_platform() + '-' + sysconfig.get_python_version()
    jpy_info_file = os.path.join(beampy_dir, jpy_distr_name + '.info')
    if force or not os.path.exists(jpy_info_file):
        beampy_lib_dir = os.path.normpath(os.path.join(beampy_dir, '..', 'lib'))
        archive_path = os.path.join(beampy_lib_dir, jpy_distr_name + '.zip')
        if not os.path.exists(archive_path):
            logging.error("Can't find binary distribution '" + archive_path + "'")
            logging.error("... of Python module 'jpy' for this system. You can try to generate one yourself.")
            logging.error("... Please go to https://github.com/bcdev/jpy and follow the build instructions")
            logging.error("... given there.")
            return 10

        #print('Found binary distribution ' + archive_path)
        #os.mkdir(os.path.join(basename)
        logging.info("unzipping '" + archive_path + "'")
        with zipfile.ZipFile(archive_path) as zf:
            zf.extractall(path=beampy_dir)


    #
    # Execute jpyutil.py to write runtime configuration:
    # - jpyconfig.properties - Configuration for Java about Python (jpy extension module)
    # - jpyconfig.py - Configuration for Python about Java (JVM)
    #

    retcode = 0

    jpyutil_file = os.path.join(beampy_dir, 'jpyutil.py')
    jpyconfig_java_file = os.path.join(beampy_dir, 'jpyconfig.properties')
    jpyconfig_py_file = os.path.join(beampy_dir, 'jpyconfig.py')
    if force or \
            not os.path.exists(jpyconfig_java_file) or \
            not os.path.exists(jpyconfig_py_file):
        if os.path.exists(jpyutil_file):
            #  Note 'jpyutil.py' has been unpacked by previous step, so we safely import it
            import jpyutil

            if not java_home:
                jre_dir = os.path.join(beampy_dir,
                                       '..',  # --> beam-python-<version>/
                                       '..',  # --> modules/
                                       '..',  # --> ${beam.home}
                                       'jre')  # --> ${beam.home}/jre
                if os.path.exists(jre_dir):
                    java_home = os.path.normpath(jre_dir)

            retcode = jpyutil.write_config_files(out_dir=beampy_dir,
                                                 java_home_dir=java_home,
                                                 req_java_api_conf=req_java,
                                                 req_py_api_conf=req_py)
        else:
            logging.error("Missing Python module '" + jpyutil_file + "' required to complete the configuration.")
            logging.error("This file should have been part of binary distribution '" + archive_path + "'.")
            retcode = 20

    return retcode


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Configures beampy, the BEAM Python interface.')
    parser.add_argument('--req_arch', default=None,
                        help='required JVM architecture, e.g. "amd64"')
    parser.add_argument('--java_home', default=None,
                        help='Java JDK or JRE installation directory')
    parser.add_argument("--log_file", action='store', default=None, help="Log file")
    parser.add_argument("--log_level", action='store', default='INFO',
                        help="Possible values: DEBUG, INFO, WARNING, ERROR")
    parser.add_argument("-j", "--req_java", action='store_true', default=False,
                        help="require that Java API configuration succeeds")
    parser.add_argument("-p", "--req_py", action='store_true', default=False,
                        help="require that Python API configuration succeeds")
    parser.add_argument('-f', '--force', action='store_true', default=False,
                        help='force overwriting of output files')
    args = parser.parse_args()

    log_level = getattr(logging, args.log_level.upper(), None)
    if not isinstance(log_level, int):
        raise ValueError('Invalid log level: %s' % log_level)

    log_format = '%(levelname)s: %(message)s'
    log_file = args.log_file
    if log_file:
        logging.basicConfig(format=log_format, level=log_level, filename=log_file, filemode='w')
    else:
        logging.basicConfig(format=log_format, level=log_level)

    try:
        retcode = _configure_beampy(java_home=args.java_home,
                                    req_arch=args.req_arch,
                                    req_java=args.req_java,
                                    req_py=args.req_py,
                                    force=args.force)
    except:
        logging.exception("Configuration of 'beampy' failed")
        retcode = 100

    if retcode == 0:
        logging.info("Configuration of 'beampy' completed")

    exit(retcode)

if __name__ == '__main__':
    exit(_main())