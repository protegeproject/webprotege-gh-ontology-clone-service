FROM openjdk:17
MAINTAINER protege.stanford.edu

ARG JAR_FILE
COPY target/${JAR_FILE} webprotege-gh-ontology-clone-service.jar
ENTRYPOINT ["java","-jar","/webprotege-gh-ontology-clone-service.jar"]