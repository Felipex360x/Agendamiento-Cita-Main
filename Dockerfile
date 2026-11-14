# Compilación
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG SKIP_TESTS=false
RUN if [ "$SKIP_TESTS" = "true" ]; then \
        mvn -B clean package -DskipTests; \
    else \
        mvn -B clean package; \
    fi

# Ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# La aplicación no se ejecuta como root dentro del contenedor.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin nicolet
USER nicolet
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
