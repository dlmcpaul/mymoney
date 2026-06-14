# syntax=docker/dockerfile:1

# Create a stage for resolving and downloading build dependencies.
ARG VERSION="development-SNAPSHOT"
ARG RELEASE="dev"
FROM azul-zulu:21 AS deps
WORKDIR /build

# Copy the mvnw wrapper with executable permissions.
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

# Download dependencies as a separate step to take advantage of Docker's caching.
# Leverage a cache mount to /root/.m2 so that subsequent builds don't have to
# re-download packages.
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

# Create a stage for building the application based on the stage with downloaded dependencies.
FROM deps AS package
ARG VERSION
ARG RELEASE

WORKDIR /build

COPY ./.git .git/
COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -Dbuild.number=${VERSION} -Dbuild.source=${RELEASE} && \
    mv target/mymoney-${VERSION}-${RELEASE}.jar target/app.jar && \
    echo "Creating a ${RELEASE} release as version ${VERSION}"

# if dev is passed as RELEASE argument then just extract
FROM package AS extract-dev
WORKDIR /build

# unpack the uber jar into it's components
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

# if prod is passed as RELEASE argument then extract and generate release.properties file
FROM package AS extract-prod
ARG VERSION
WORKDIR /build

# unpack the uber jar into it's components and set release properties
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted && \
    echo "release.version=${VERSION}" > ./target/extracted/application/BOOT-INF/classes/release.properties

#Choose prod or dev layer
FROM extract-${RELEASE} AS pre-final

# Create the runtime image using a minimal base image
# and copy only necessary files from the builder image
FROM azul-zulu:21-jre-headless AS final

# Copy the executable from the "pre-final" stage.
COPY --from=pre-final build/target/extracted/dependencies/ ./
COPY --from=pre-final build/target/extracted/spring-boot-loader/ ./
COPY --from=pre-final build/target/extracted/snapshot-dependencies/ ./
COPY --from=pre-final build/target/extracted/application/ ./

EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]