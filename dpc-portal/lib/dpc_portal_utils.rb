# frozen_string_literal: true

# Shared functionality
module DpcPortalUtils
  def my_protocol_host
    env = ENV.fetch('ENV', nil)
    case env
    when 'local'
      'http://localhost:3100'
    when 'prod-sbx'
      'https://sandbox.dpc.cms.gov'
    else
      "https://#{env}.dpc.cms.gov"
    end
  end
end
