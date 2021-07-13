#!/bin/sh -x
set -e
CUR_DIR="$( cd -P "$( dirname "$0" )" && pwd )"
java -cp "$CUR_DIR/modules/*:$CUR_DIR/lib/*" -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT -Dsnap.home="$HOME/.snap" -Djava.net.useSystemProxies=true -Xmx4G org.esa.snap.runtime.Launcher "$@"
