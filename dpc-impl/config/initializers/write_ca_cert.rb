# frozen_string_literal: true

if (ENV.fetch('ENV') == "prod-sbx" || ENV.fetch('ENV') == "prod")
  File.write('ca.crt', Base64.decode64(ENV.fetch('DPC_CA_CERT')))
end
