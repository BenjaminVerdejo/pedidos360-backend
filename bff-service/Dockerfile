# syntax=docker/dockerfile:1
# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen final minima ----------
# Solo JRE (sin compilador ni Maven) => menos paquetes, menos vulnerabilidades
FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="bff-service" \
      org.opencontainers.image.source="https://github.com/BenjaminVerdejo/pedidos360-backend"
WORKDIR /app
# Usuario sin privilegios (CIS Docker Benchmark 4.1: no ejecutar como root)
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["wget", "-qO-", "http://localhost:8081/actuator/health"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
