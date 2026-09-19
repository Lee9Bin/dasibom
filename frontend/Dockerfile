# Next.js standalone is built locally, so the EC2 only runs the finished app.
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000 NODE_OPTIONS=--max-old-space-size=144
COPY --chown=node:node .next/standalone ./
COPY --chown=node:node .next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
