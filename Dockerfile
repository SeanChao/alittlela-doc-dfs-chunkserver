FROM gradle:7.1-jdk16
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist

ENTRYPOINT [ "./app/build/install/app/bin/app" ]
