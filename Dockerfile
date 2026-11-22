# Compilación
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Las dependencias se descargan en una capa aparte: solo se repite si cambia el pom.xml.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
ARG SKIP_TESTS=false
RUN if [ "$SKIP_TESTS" = "true" ]; then \
        mvn -B clean package -DskipTests; \
    else \
        mvn -B clean package; \
    fi

# Ejecución
FROM eclipse-temurin:22-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# La aplicación no se ejecuta como root dentro del contenedor.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin nicolet
USER nicolet
EXPOSE 8080
# La JVM respeta el límite de memoria del contenedor y termina con un código claro si se agota.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
