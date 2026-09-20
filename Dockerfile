FROM eclipse-temurin:25

COPY build/libs/WebUntisNotifier.jar WebUntisNotifier.jar

ENTRYPOINT ["java", "-jar", "/WebUntisNotifier.jar"]