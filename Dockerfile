FROM openjdk:16 AS build

MAINTAINER Christopher A. Mosher <cmosher01@gmail.com>

USER root
ENV HOME /root
WORKDIR $HOME

COPY gradle/ gradle/
COPY gradlew ./
RUN ./gradlew --version

COPY settings.gradle ./
COPY build.gradle ./
COPY src/ ./src/

RUN ./gradlew -i build
RUN ./gradlew dependencies --configuration=compileClasspath



FROM tomcat:10-jdk16 AS run

USER root
ENV HOME /root
WORKDIR $HOME

ENV JAVA_OPTS --enable-preview

COPY src/main/tomcat /usr/local/tomcat/conf

COPY --from=build /root/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
