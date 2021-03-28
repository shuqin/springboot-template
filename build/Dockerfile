FROM java:8-alpine
ADD springboot-template-1.0.jar app.jar
EXPOSE 8888
VOLUME /tmp
ENTRYPOINT ["java", "-Xbootclasspath/a:./", "-Dserver.port=8888", "-jar","/app.jar"]
