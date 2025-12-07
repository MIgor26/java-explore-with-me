FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

VOLUME /tmp

EXPOSE 8080

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]