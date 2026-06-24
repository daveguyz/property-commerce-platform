FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY infrastructure/service-discovery/pom.xml infrastructure/service-discovery/pom.xml
RUN ./mvnw dependency:go-offline -pl infrastructure/service-discovery -q 2>/dev/null || true
COPY infrastructure/service-discovery/ infrastructure/service-discovery/
RUN ./mvnw package -pl infrastructure/service-discovery -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S staysphere && adduser -S staysphere -G staysphere
USER staysphere
COPY --from=builder /workspace/infrastructure/service-discovery/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
