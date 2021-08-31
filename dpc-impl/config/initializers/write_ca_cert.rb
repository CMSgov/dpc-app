# frozen_string_literal: true

File.write('ca.crt', ENV.fetch('DPC_CA_CERT'))