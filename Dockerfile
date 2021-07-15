FROM gradle:7.1-jdk16
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist

RUN mkdir /app
RUN ls app/build/libs
COPY app/build/install/app /app/

ENTRYPOINT [ "/app/bin/app" ]
