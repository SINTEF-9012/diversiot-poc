FROM ubuntu:16.04

RUN apt-get update
# To build posix
RUN apt-get -y install build-essential

# To build java
RUN apt-get -y install default-jdk
RUN apt-get -y install maven

# Nice tool to make fake serial ports
RUN apt-get -y install socat

CMD /bin/bash