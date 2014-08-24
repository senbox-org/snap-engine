import sys
import sysconfig
import os
import os.path
import platform
import argparse
import zipfile
import subprocess


def _configure_beampy(req_arch):

    warnings = []
    errors = []

    act_arch = platform.machine()

    #print(act_arch)
    is64 = sys.maxsize > 2 ** 31 - 1
    #print('is64: ' + str(is64))

    if act_arch.lower() != req_arch.lower():
        warnings.append("Architectures don't match: Java requires " + req_arch + ", current Python is " + act_arch)

    beampy_dir = os.path.dirname(os.path.abspath(__file__))

    jpy_distr_name = 'jpy.' + sysconfig.get_platform() + '-' + sysconfig.get_python_version()
    jpy_info_file = os.path.join(beampy_dir, jpy_distr_name + '.info')
    if not os.path.exists(jpy_info_file):

        #
        # Extract jpy binary distribution
        #

        beampy_lib_dir = os.path.normpath(os.path.join(beampy_dir, '..', 'lib'))

        archive_path = os.path.join(beampy_lib_dir, jpy_distr_name + '.zip')
        if not os.path.exists(archive_path):
            errors.append("Can't find binary distribution '" + archive_path + "'")
            return errors, warnings

        #print('Found binary distribution ' + archive_path)

        #os.mkdir(os.path.join(basename)
        with zipfile.ZipFile(archive_path) as zf:
            zf.extractall(path=beampy_dir)

    jpy_properties_file = os.path.join(beampy_dir, 'jpyconfig.properties')
    if not os.path.exists(jpy_properties_file):

        #
        # Execute jpyutil.py to write runtime configuration:
        #   jpyconfig.properties - Configuration for Java about Python (jpy extension module)
        #   jpyconfig.py - Configuration for Python about Java (JVM)
        #

        pp = os.environ.get('PYTHONPATH')
        pp = os.pathsep.join([beampy_dir, pp]) if pp else beampy_dir
        print("Setting PYTHONPATH to " + pp)
        os.environ['PYTHONPATH'] = pp

        jre_dir = os.path.join(beampy_dir,
                               '..',  # --> beam-python-<version>/
                               '..',  # --> modules/
                               '..',  # --> ${beam.home}
                               'jre'  # --> ${beam.home}/jre
        )

        #print('JRE: ' + str(jre_dir))

        jpyutil_file = os.path.join(beampy_dir, 'jpyutil.py')
        jpyutil_cmd = [sys.executable, jpyutil_file, '-f']
        if os.path.exists(jre_dir):
            jpyutil_cmd += ['--java_home', jre_dir]

        retcode = subprocess.call(jpyutil_cmd)
        if retcode:
            errors.append('jpy configuration failed: retcode='+retcode)

    return errors, warnings


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Configures beampy, the BEAM Python interface.')
    parser.add_argument('arch', nargs=1,
                        help='Required JVM architecture, e.g. "amd64"')
    args = parser.parse_args()
    errors, warnings = _configure_beampy(args.arch[0])
    with open('beampy-config.log', 'w') as f:
        for error in errors:
            f.write("error: " + error + "\n")
        for warning in warnings:
            f.write("warning: " + warning + "\n")
    exit(0 if not len(errors) else 1)


