# Multi-stage build — builder stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and root POM for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy shared modules
COPY shared/common-dto/pom.xml shared/common-dto/pom.xml
COPY shared/common-events/pom.xml shared/common-events/pom.xml
COPY shared/common-security/pom.xml shared/common-security/pom.xml

# Copy this service pom
COPY services/messaging-service/pom.xml services/messaging-service/pom.xml

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -pl shared/common-dto,shared/common-events,shared/common-security,services/messaging-service -am -q 2>/dev/null || true

# Copy source
COPY shared/ shared/
COPY services/messaging-service/ services/messaging-service/

# Build
RUN ./mvnw package -pl services/messaging-service -am -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: run as non-root
RUN addgroup -S staysphere && adduser -S staysphere -G staysphere
USER staysphere

# Copy layered jar
COPY --from=builder /workspace/services/messaging-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "-jar", "app.jar"]
