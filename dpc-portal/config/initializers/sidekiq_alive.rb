SidekiqAlive.setup do |config|
  # ==> Server port
  # Port to bind the server.
  # Can also be set with the environment variable SIDEKIQ_ALIVE_PORT.
  # default: 7433
  #
     config.port = 7435
  # ==> Server path
  # HTTP path to respond to.
  # Can also be set with the environment variable SIDEKIQ_ALIVE_PATH.
  # default: '/'
  #
  #   config.path = '/'
  # ==> Liveness key
  # Key to be stored in Redis as probe of liveness
  # default: "SIDEKIQ::LIVENESS_PROBE_TIMESTAMP"
  #
  #   config.liveness_key = "SIDEKIQ::LIVENESS_PROBE_TIMESTAMP"
  # ==> Time to live
  # Time for the key to be kept by Redis.
  # Here is where you can set de periodicity that the Sidekiq has to probe it is working
  # Time unit: seconds
  # default: 10 * 60 # 10 minutes
  #
  #   config.time_to_live = 10 * 60
  # ==> Callback
  # After the key is stored in redis you can perform anything.
  # For example a webhook or email to notify the team
  # default: proc {}
  #
  #    require 'net/http'
  #    config.callback = proc { Net::HTTP.get("https://status.com/ping") }
  # ==> Queue Prefix
  # SidekiqAlive will run in a independent queue for each instance/replica
  # This queue name will be generated with: "#{queue_prefix}-#{hostname}.
  # You can customize the prefix here.
  # default: :sidekiq_alive
  #
  #    config.queue_prefix = :other
  # ==> Rack server
  # Web server used to serve an HTTP response.
  # Can also be set with the environment variable SIDEKIQ_ALIVE_SERVER.
  # default: 'webrick'
  #
  #    config.server = 'puma'
end