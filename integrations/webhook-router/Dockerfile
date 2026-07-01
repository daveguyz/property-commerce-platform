FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw package -pl integrations/webhook-router -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/integrations/webhook-router/target/*.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java","-jar","app.jar"]
