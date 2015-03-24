## Building tmate for Ubuntu 10.04

Since CloudFoundry containers are currently based on Ubuntu 10.04 64bit (lucid64) containers, the binaries should be compiled to be compatible in that environment.
One way to achieve this is to build binaries in a Ubuntu 10.04 based docker container.

### building `tmate` in docker

starting a named docker container
```
docker run -it --name lucid cloudfoundry/lucid64 bash
```

restarting and attaching to this named container
```
docker restart lucid
docker attach lucid
# press enter twice
```

#### Commands to run in the container for building tmate

Pre-requisites
```
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys A1715D88E1DF1F24
sudo apt-get update
sudo apt-get install git-core build-essential pkg-config libtool libevent-dev libncurses-dev zlib1g-dev automake libssh-dev cmake ruby
```

Build libevent-2.0 from source
```
wget --no-check-certificate https://sourceforge.net/projects/levent/files/libevent/libevent-2.0/libevent-2.0.22-stable.tar.gz
tar zxvf libevent-2.0.22-stable.tar.gz
cd libevent-2.0.22-stable
./configure --prefix=/opt/tmate
make
make install
cd ..
```

Build tmate from source
```
mkdir tmate-src
cd tmate-src/
git clone https://github.com/nviennot/tmate

cd tmate
./configure --prefix=/opt/tmate LDFLAGS=-L/opt/tmate/lib CPPFLAGS=-I/opt/tmate/include
make
make install
cd ..
```

Copy the required files inside the container to a single directory for transfer
```
mkdir /opt/tmate/tmate-binary
cp /opt/tmate/bin/tmate /opt/tmate/lib/libevent-2.0.so.5 /opt/tmate/tmate-binary/
```

Copying timeout command to be used in tmate-server.sh script for timeout control
```
cd /tmp
wget http://archive.ubuntu.com/ubuntu/pool/universe/t/tct/timeout_1.19-1_amd64.deb
dpkg-deb -x timeout_1.19-1_amd64.deb .
cp /tmp/usr/bin/timeout /opt/tmate/tmate-binary/
```

Copying files from docker container (run command on host)
```docker cp lucid:/opt/tmate/tmate-binary .```
You should then have a directory `tmate-binary` with 3 files inside it: `tmate`, `libevent-2.0.so.5` and `timeout`.

update tmate.tar.gz file included in `java-buildpack-diagnostics-app`:
```
mv tmate-binary tmate
tar zcvf tmate.tar.gz tmate
cp tmate.tar.gz java-buildpack-diagnostics-app/src/main/resources/
```
