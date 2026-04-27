# Specify Temurin and Tomcat image versions explicitly to keep them in
# sync with each other.
# Temurin:
#     https://adoptium.net/installation/containers
#     eclipse-temurin:<java-version>-<java-type>-<os-version>
#     <java-version>, use major version number, such as "25"
#     <java-type> is "jdk"
#     <os-version> is Ubuntu name, such as "noble"
# Tomcat:
#     https://hub.docker.com/_/tomcat/
#     <tomcat-version>-<java-type><java-version>-temurin-<os-version>
#     <tomcat-version>, use major version number, such as "11"
#     <java-type> is "jdk"
#     <java-version>, use major version number, such as "25" (use same one as for Temurin)
#     <os-version> is Ubuntu name, such as "noble" (use same one as for Temurin)

FROM eclipse-temurin:25-jdk-noble AS build

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



FROM tomcat:11-jdk25-temurin-noble AS run

USER root
ENV HOME /root
WORKDIR $HOME

# Noto Sans font is used for text measuring to determine
# wrapping width of person rectangles in svg chart
RUN apt-get update && apt-get install -y fonts-noto

COPY src/main/tomcat /usr/local/tomcat/conf

COPY --from=build /root/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

VOLUME /root
