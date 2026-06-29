FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY infrastructure/config-server/pom.xml infrastructure/config-server/pom.xml
RUN ./mvnw dependency:go-offline -pl infrastructure/config-server -q 2>/dev/null || true
COPY infrastructure/config-server/ infrastructure/config-server/
RUN ./mvnw package -pl infrastructure/config-server -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S propertycommerce && adduser -S propertycommerce -G propertycommerce
USER propertycommerce
COPY --from=builder /workspace/infrastructure/config-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
