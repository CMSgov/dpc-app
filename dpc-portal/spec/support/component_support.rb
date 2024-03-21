# frozen_string_literal: true

module ComponentSupport
  # Mocks an org from dpc-api
  class MockOrg
    attr_accessor :name, :npi, :to_param, :path_id

    def initialize(row_count = 0)
      @to_param = @path_id = '2'
      @name = 'Health'
      @npi = '11111'
      @row_count = row_count
      @created = '2023-12-15 17:01'
      @expires = '2023-12-16 17:01'
      @guid = '99790463-de1f-4f7f-a529-3e4f59dc713'
    end

    def client_tokens
      tokens = []
      @row_count.times do |index|
        tokens << { 'label' => "Token #{index + 1}",
                    'id' => "token-id-#{index + 1}",
                    'expiresAt' => @expires,
                    'createdAt' => @created }
      end
      tokens
    end

    def public_keys
      tokens = []
      @row_count.times do |index|
        tokens << { 'label' => "Key #{index + 1}",
                    'id' => "key-id-#{index + 1}",
                    'createdAt' => @created }
      end
      tokens
    end

    def public_ips
      tokens = []
      @row_count.times do |index|
        tokens << { 'label' => "IP Addr #{index + 1}",
                    'id' => "addr-id-#{index + 1}",
                    'ip_addr' => "127.0.0.#{index + 10}",
                    'createdAt' => @created }
      end
      tokens
    end
  end

  # handles whitespace for text matching
  def normalize_space(str)
    str.gsub(/^ +/, '').gsub("\n", '')
  end
end
