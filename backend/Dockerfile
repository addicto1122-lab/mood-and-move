
# ==========================================
# 1단계: Spring Boot 빌드
# ==========================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

# Gradle로 실행 가능한 JAR 생성
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# 일반 JAR(-plain.jar)를 제외하고 실행용 JAR 복사
RUN find build/libs -maxdepth 1 -type f \
    -name "*.jar" ! -name "*-plain.jar" \
    -exec cp {} /app/app.jar \; \
    && test -s /app/app.jar


# ==========================================
# 2단계: Spring Boot 실행
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
