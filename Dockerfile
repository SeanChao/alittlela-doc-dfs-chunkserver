FROM gradle:7.1-jdk16
COPY . /src
WORKDIR /src
RUN gradle installDist --no-daemon

ENTRYPOINT [ "./app/build/install/app/bin/app" ]
