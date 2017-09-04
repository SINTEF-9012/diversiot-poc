FROM ubuntu:16.04

RUN apt-get update
# To build posix
RUN apt-get -y install build-essential

# To build java
RUN apt-get -y install default-jdk
RUN apt-get -y install maven

# Nice tool to make fake serial ports
RUN apt-get -y install socat

# Get the latest ThingML compiler
ADD http://thingml.org/dist/ThingML2CLI.jar /root/thingmlcli.jar

# Prepare Maven by downloading some dependencies so we don't need to do it later
COPY empty.thingml /root/empty.thingml
RUN java -jar /root/thingmlcli.jar --compiler java --source /root/empty.thingml --output /root/empty/
RUN mvn --file /root/empty/ clean install

CMD /bin/bash
