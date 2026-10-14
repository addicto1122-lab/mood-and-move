
# ==========================================
# 1단계: React 빌드
# ==========================================
FROM node:22-alpine AS build

WORKDIR /app

# 의존성 설치
COPY package*.json ./
RUN npm ci

# 프로젝트 복사 및 빌드
COPY . .
RUN npm run build


# ==========================================
# 2단계: Nginx 실행
# ==========================================
FROM nginx:stable-alpine

# React 빌드 결과 복사
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx 설정 적용
COPY nginx.conf /etc/nginx/conf.d/default.conf

# HTTP 포트
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]
