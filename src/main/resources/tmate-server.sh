#!/bin/bash
# script for launching tmate >= 1.8.10 in server mode
# written for Ubuntu 10.04 since CloudFoundry execution environment uses it
# author: Lari Hotari , https://github.com/lhotari
#
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
# Ubuntu 10.04 requires newer libevent version, assume that the libraries are in the script directory
export LD_LIBRARY_PATH=$SCRIPTDIR
TMATE="$SCRIPTDIR/tmate -S /tmp/tmate.sock"

case "$1" in
    start)
        tmate_ssh=$($TMATE display -p '#{tmate_ssh}' 2>&1)
        if [[ $tmate_ssh =~ failed ]]; then
            # create ssh key if it doesn't exist
            # assume that this script is installed in a directory under the homedir
            # CloudFoundry sets HOME to /home/vcap/app which isn't obeyed by Java or tmate
            SSH_HOME=`dirname $SCRIPTDIR`/.ssh
            [ ! -d $SSH_HOME ] && mkdir $SSH_HOME && chmod 0700 $SSH_HOME
            [ ! -f $SSH_HOME/id_rsa ] && ssh-keygen -q -t rsa -f $SSH_HOME/id_rsa -N ""
            $TMATE -vvv new-session -d 2>&1
            $SCRIPTDIR/timeout 15 $TMATE wait tmate-ready 2>&1
            $TMATE display -p '#{tmate_ssh}' 2>&1
        else
            echo $tmate_ssh
        fi
        ;;
    stop)
        $TMATE kill-server 2>&1
        ;;
    status)
        $TMATE info 2>&1
        $TMATE display -p '#{tmate_ssh}' 2>&1
        ;;
    *)
        echo $"Usage: $0 {start|stop|status}"
        exit 1
        ;;
esac
