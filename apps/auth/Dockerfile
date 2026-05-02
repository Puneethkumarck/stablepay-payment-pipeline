FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY main/build/libs/stablepay-auth-*.jar app.jar

RUN addgroup -S stablepay && adduser -S -G stablepay stablepay \
    && chown stablepay:stablepay /app/app.jar
USER stablepay

EXPOSE 9000

HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=30s \
    CMD curl -fs http://localhost:9000/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
