DpcWebsite::Application.config.session_store :redis_store, {
  servers: [
    { host: Rails.application.config.redis_host,
    port: 6379, db: 0, namespace: "session" },
  ],
  expire_after: 1.day
}