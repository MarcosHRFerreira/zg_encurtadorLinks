# ---- Build stage ----
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile

# Copy source and build with API base URL injection
COPY . .
ARG API_BASE_URL
ENV API_BASE_URL=${API_BASE_URL}
RUN yarn build

# ---- Runtime stage ----
FROM nginx:alpine

# Copy built assets (Angular application builder outputs to dist/<app>/browser)
COPY --from=builder /app/dist/frontend /usr/share/nginx/html

# Nginx config with SPA fallback
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80