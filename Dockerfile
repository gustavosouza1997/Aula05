FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY Servidor.java .
RUN javac Servidor.java

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/*.class .
COPY A.txt .

EXPOSE 5000
CMD ["java", "Servidor"]
