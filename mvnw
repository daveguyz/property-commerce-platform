#!/bin/sh
# Maven wrapper — downloads Maven if not present
MAVEN_VERSION=3.9.6
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="${MAVEN_HOME}/bin/mvn"

if [ ! -f "${MAVEN_BIN}" ]; then
    mkdir -p "${MAVEN_HOME}"
    DOWNLOAD_URL="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    curl -fsSL "${DOWNLOAD_URL}" | tar -xz -C "${HOME}/.m2/wrapper/dists/"
    mv "${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}" "${MAVEN_HOME}"
fi
exec "${MAVEN_BIN}" "$@"
