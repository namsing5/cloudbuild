# Use the official maven/Java 8 image to create a build artifact.
# https://hub.docker.com/_/maven
FROM maven:3.5-jdk-8-alpine as builder
#FROM jdk-11.0.9_11-alpine as builder

# Copy local code to the container image.
#Copy settings.xml to parent directory in this case to complete folder to access artifactory
WORKDIR /app
COPY pom.xml .
COPY src ./src
#COPY settings.xml .

# Build a release artifact.
RUN mvn package -DskipTests

# Use AdoptOpenJDK for base image.
# It's important to use OpenJDK 8u191 or above that has container support enabled.
# https://hub.docker.com/r/adoptopenjdk/openjdk8
# https://docs.docker.com/develop/develop-images/multistage-build/#use-multi-stage-builds
FROM adoptopenjdk/openjdk8:jdk8u202-b08-alpine-slim
#FROM adoptopenjdk/openjdk11:jdk-11.0.9_11-alpine

# Copy the jar to the production image from the builder stage.
COPY --from=builder /app/target/eventmeshtopubsubproducer-*.jar /eventmeshtopubsubproducer.jar

# Run the web service on container startup.
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-Dserver.port=${CONTAINER_PORT}","-jar","/eventmeshtopubsubproducer.jar"]
