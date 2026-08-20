# config/initializers/httplog.rb
return unless defined?(HttpLog)

HttpLog.configure do |config|
  config.url_blacklist_pattern = /datadog-agent/
end
