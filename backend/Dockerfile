# Built for linux/arm64 in production (ENG-5); the platform is chosen by the build command.
FROM eclipse-temurin:25-jdk@sha256:884a2f8692cc1e2c7e21f95914adf0ae0eafee9a7eb663a12169cc44468256ec AS build
# Without unzip the Maven wrapper fetches the .tar.gz instead, which fails its zip checksum.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B dependency:go-offline
COPY src src
# Tests need Docker for Testcontainers and run in `./mvnw verify`, not in the image build.
RUN sh mvnw -B package -DskipTests && cp target/niskora-backend-*.jar app.jar

FROM eclipse-temurin:25-jre@sha256:945247f9d092703e0c6be92243ef3c51640229e23ab34d353c8dcdc73e6cd4e6
# Sent on every response (ENG-92). CI passes the commit hash; a local build says "dev".
ARG BUILD_ID=dev
ENV NISKORA_BUILD_ID=${BUILD_ID}
RUN useradd --system --no-create-home niskora
USER niskora
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
