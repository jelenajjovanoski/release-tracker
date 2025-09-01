FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY .. .
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /usr/local/release-tracker
COPY --from=build /build/target/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java","-jar","app.jar"]
