FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_API_BASE_URL
ARG VITE_OPENAPI_SCHEMA_URL

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_OPENAPI_SCHEMA_URL=${VITE_OPENAPI_SCHEMA_URL}

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build-only

FROM nginx:1.27-alpine

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
