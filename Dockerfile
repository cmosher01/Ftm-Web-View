FROM openjdk:14 AS build

MAINTAINER Christopher A. Mosher <cmosher01@gmail.com>

USER root
ENV HOME /root
WORKDIR $HOME

RUN echo "org.gradle.daemon=false" >gradle.properties

COPY gradle/ gradle/
COPY gradlew ./
RUN ./gradlew --version

COPY settings.gradle ./
COPY build.gradle ./
COPY src/ ./src/

RUN ./gradlew build



FROM tomcat:10-jdk14 AS run

USER root
ENV HOME /root
WORKDIR $HOME

ENV JAVA_OPTS --enable-preview

COPY src/main/tomcat /usr/local/tomcat/conf

COPY --from=build /root/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
