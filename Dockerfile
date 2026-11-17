# 배포용 이미지 (Vercel 컨테이너 런타임 등). 빌드 단계에서 테스트는 돌리지 않는다(CI가 담당).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test && cp build/libs/*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
# 컨테이너 밖에서 접속할 수 있게 모든 주소에 바인딩한다. 포트는 PORT 환경변수, 없으면 80(Vercel 기본).
ENV SERVER_ADDRESS=0.0.0.0 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:TieredStopAtLevel=1"
EXPOSE 80
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-80} -jar app.jar"]
