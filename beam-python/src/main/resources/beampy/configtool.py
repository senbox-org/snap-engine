import sys
import sysconfig
import os
import os.path
import platform
import argparse
import zipfile
import subprocess


def _configure_beampy(java_home=None, req_arch=None, force=False):
    warnings = []
    errors = []

    if req_arch:
        req_arch = req_arch.lower()
        act_arch = platform.machine().lower()
        if req_arch != act_arch:
            warnings.append("architecture requirement not met: Python is " + act_arch + " but JVM requires " + req_arch)
        is64 = sys.maxsize > 2 ** 31 - 1
        if is64 and not req_arch in ('amd64', 'ia64', 'x64'):
            warnings.append("architecture requirement not met: Python is 64-bit but JVM requires " + req_arch)

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
            errors.append("can't find binary distribution '" + archive_path + "'")
            return errors, warnings
        #print('Found binary distribution ' + archive_path)
        #os.mkdir(os.path.join(basename)
        with zipfile.ZipFile(archive_path) as zf:
            zf.extractall(path=beampy_dir)

    #
    # Execute jpyutil.py to write runtime configuration:
    #   jpyconfig.properties - Configuration for Java about Python (jpy extension module)
    #   jpyconfig.py - Configuration for Python about Java (JVM)
    #
    jpy_properties_file = os.path.join(beampy_dir, 'jpyconfig.properties')
    if force or not os.path.exists(jpy_properties_file):
        if not java_home:
            jre_dir = os.path.join(beampy_dir,
                                   '..',  # --> beam-python-<version>/
                                   '..',  # --> modules/
                                   '..',  # --> ${beam.home}
                                   'jre') # --> ${beam.home}/jre
            if os.path.exists(jre_dir):
                java_home = os.path.normpath(jre_dir)
        #print('java_home: ' + str(java_home))
        jpyutil_file = os.path.join(beampy_dir, 'jpyutil.py')
        jpyutil_cmd = [sys.executable, jpyutil_file, '-f']
        if java_home:
            jpyutil_cmd += ['--java_home', java_home]
        retcode = subprocess.call(jpyutil_cmd)
        if retcode:
            errors.append('jpy configuration failed with return code ' + str(retcode))

    return errors, warnings


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Configures beampy, the BEAM Python interface.')
    parser.add_argument('-a', '--arch',
                        help='required JVM architecture, e.g. "amd64"')
    parser.add_argument('-j', '--java_home',
                        help='Java JDK or JRE installation directory')
    parser.add_argument('-f', '--force', action='store_true',
                        help='force overwriting of output files')
    args = parser.parse_args()
    errors, warnings = _configure_beampy(java_home=args.java_home, req_arch=args.arch, force=args.force)
    with open('configtool.log', 'w') as f:
        for error in errors:
            f.write("error: " + error + "\n")
        for warning in warnings:
            f.write("warning: " + warning + "\n")
    exit(0 if not len(errors) else 1)


