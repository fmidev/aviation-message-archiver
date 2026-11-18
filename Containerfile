# Build stage
FROM ghcr.io/bell-sw/liberica-runtime-container:jdk-21-stream-musl@sha256:c7b193f579e52069d68aab0159c93a304c59183e37cf4ba4b95092e7ff90c2d4 AS build

WORKDIR /app

# Copy build files and build.
# Tests are skipped because we assume they have succeeded in a CI pipeline before this build.
# Maven caches (repository, wrapper) can be provided via --volume at build time.
COPY mvnw pom.xml ./
RUN --mount=type=bind,source=.git,target=./.git \
    --mount=type=bind,source=.m2/settings.xml,target=/root/.m2/settings.xml \
    --mount=type=bind,source=.mvn,target=./.mvn \
    --mount=type=bind,source=src,target=./src \
    ./mvnw package -DskipTests -B

# Runtime stage
FROM ghcr.io/bell-sw/liberica-runtime-container:jre-21-stream-musl@sha256:7088f7488269c3355fcd77b3ed0f08e1cbd156fb6621b289219dacd71cab987d

LABEL org.opencontainers.image.title="Aviation Message Archiver" \
      org.opencontainers.image.description="A service application for operationally archiving aviation weather messages into database" \
      org.opencontainers.image.vendor="Finnish Meteorological Institute" \
      org.opencontainers.image.source="https://github.com/fmidev/aviation-message-archiver" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*-bundle.jar app.jar

# OpenShift runs containers with arbitrary UIDs but always with GID 0, so g=u ensures access
RUN chown -R 1001:0 /app && \
    chmod -R g=u /app

# Run as non-root for local Podman/Docker. OpenShift will override this with an arbitrary UID.
USER 1001

# Default JVM options for container environment
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
# Default data directory root inside container
ENV PRODUCTION_LINE_DIRECTORY_ROOT="/data"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
