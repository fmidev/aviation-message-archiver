# Build stage
FROM ghcr.io/bell-sw/liberica-runtime-container:jdk-21-stream-musl@sha256:107c450eb81981175a0198d92bb226797743a4cf1d54dab4f07b1b00d8f21c7d AS build

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
FROM ghcr.io/bell-sw/liberica-runtime-container:jre-21-stream-musl@sha256:d65e84088e3feb84238ecf05408c9afb0ee814871b246d33ed4c367d030a2478

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
