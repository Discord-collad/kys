# =========================================================
# Dockerfile multi-stage para KYS (Spring Boot 3.2 / Java 21)
# =========================================================
# Build con Temurin JDK 21, runtime con Temurin JRE 21 (imagen más chica).

# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copiamos primero el wrapper + pom para cachear las dependencias
COPY .mvn ./.mvn
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x ./mvnw \
 && ./mvnw -B -q -DskipTests dependency:go-offline

# Ahora sí, todo el código fuente
COPY src ./src

# Generamos el JAR (sin tests, ya los corrés en CI/local con Maven)
RUN ./mvnw -B -DskipTests package \
 && cp target/kys-0.0.1-SNAPSHOT.jar /app/kys-app.jar

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="KYS"
WORKDIR /app

# Usuario no-root por seguridad (Render lo ejecuta como root si no, ojo)
RUN groupadd --system --gid 1001 kys \
 && useradd  --system --uid 1001 --gid kys kys \
 && mkdir -p /app \
 && chown -R kys:kys /app
USER kys

COPY --from=build /app/kys-app.jar /app/kys-app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "if [ -n \"$SPRING_DATASOURCE_URL\" ] && [ \"${SPRING_DATASOURCE_URL#postgresql://}\" != \"$SPRING_DATASOURCE_URL\" ]; then export SPRING_DATASOURCE_URL=\"jdbc:$SPRING_DATASOURCE_URL\"; elif [ -n \"$SPRING_DATASOURCE_URL\" ] && [ \"${SPRING_DATASOURCE_URL#postgres://}\" != \"$SPRING_DATASOURCE_URL\" ]; then export SPRING_DATASOURCE_URL=\"jdbc:postgresql://${SPRING_DATASOURCE_URL#postgres://}\"; fi; exec java $JAVA_OPTS -jar /app/kys-app.jar \"$@\"", "--"]