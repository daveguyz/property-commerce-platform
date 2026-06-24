FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY infrastructure/api-gateway/pom.xml infrastructure/api-gateway/pom.xml
RUN ./mvnw dependency:go-offline -pl infrastructure/api-gateway -q 2>/dev/null || true
COPY infrastructure/api-gateway/ infrastructure/api-gateway/
RUN ./mvnw package -pl infrastructure/api-gateway -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S staysphere && adduser -S staysphere -G staysphere
USER staysphere
COPY --from=builder /workspace/infrastructure/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
