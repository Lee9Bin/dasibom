FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/dasibom-api-0.1.0.jar /app/app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx160m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss256k -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
