# frozen_string_literal: true

# Shared functionality
module DpcPortalUtils
  def my_protocol_host
    env = ENV.fetch('ENV', nil)
    case env
    when 'local'
      'http://localhost:3100'
    else
      host_name = ENV.fetch('HOST_NAME', nil)
      Rails.logger.error 'HOST_NAME is not set by env' if host_name.nil?
      "https://#{host_name}"
    end
  end
end
