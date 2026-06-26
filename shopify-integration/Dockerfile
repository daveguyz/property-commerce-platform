# ============================================================
# StaySphere AOS — shopify-integration
# Multi-stage build: packages theme files from classpath
# then builds the Spring Boot JAR.
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Maven wrapper + root POM
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Shared module POMs (dependency caching layer)
COPY shared/common-dto/pom.xml     shared/common-dto/pom.xml
COPY shared/common-events/pom.xml  shared/common-events/pom.xml
COPY shared/common-security/pom.xml shared/common-security/pom.xml

# shopify-integration POM (note: root-level module, not under services/)
COPY shopify-integration/pom.xml shopify-integration/pom.xml

# Download deps (cached)
RUN ./mvnw dependency:go-offline \
      -pl shared/common-dto,shared/common-events,shared/common-security,shopify-integration \
      -am -q 2>/dev/null || true

# Copy source
COPY shared/             shared/
COPY shopify-integration/ shopify-integration/

# Build (theme-files/ must be populated before this step in CI)
RUN ./mvnw package -pl shopify-integration -am -DskipTests -q

# ── Runtime ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S staysphere && adduser -S staysphere -G staysphere
USER staysphere

COPY --from=builder /workspace/shopify-integration/target/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
