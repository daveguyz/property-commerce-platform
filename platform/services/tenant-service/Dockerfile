FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw package -pl platform/services/tenant-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/platform/services/tenant-service/target/*.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java","-jar","app.jar"]
