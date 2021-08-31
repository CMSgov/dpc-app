# frozen_string_literal: true

File.write('ca.crt', Base64.decode64(ENV.fetch('DPC_CA_CERT')))