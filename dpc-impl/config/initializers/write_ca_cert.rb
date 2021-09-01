# frozen_string_literal: true

if !(Rails.env.development? || Rails.env.test?)
  File.write('ca.crt', Base64.decode64(ENV.fetch('DPC_CA_CERT')))
end