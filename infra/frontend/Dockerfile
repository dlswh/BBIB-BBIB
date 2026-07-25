FROM node:22-alpine AS build

WORKDIR /app

RUN corepack enable

COPY web/package.json web/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY web/ ./

ARG VITE_API_BASE_URL
ARG VITE_USE_MOCK=true
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_USE_MOCK=${VITE_USE_MOCK}

RUN pnpm build

FROM nginx:1.27-alpine

COPY infra/frontend/container-nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
