FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

RUN mkdir -p /var/app/uploads /var/log/backbackback \
    && chown -R app:app /app /var/app/uploads /var/log/backbackback

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
