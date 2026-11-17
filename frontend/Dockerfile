# The frontend image is Caddy with the built React app inside it (PLT-5, FE-39).
# Built for linux/arm64 in production (ENG-5); the platform is chosen by the build command.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
RUN corepack enable
WORKDIR /build
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM caddy:2@sha256:4851e54a46b206517c574821bb5cc67f170d2156041e9fbc4c9193b45e84a75b
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /build/dist /srv
