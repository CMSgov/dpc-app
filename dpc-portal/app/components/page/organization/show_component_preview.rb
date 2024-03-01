# frozen_string_literal: true

module Page
  module Organization
    # Previews the Organization#show page
    class ShowComponentPreview < ViewComponent::Preview
      # @param row_count
      def default(row_count: 2)
        org = MockOrg.new(row_count.to_i)
        org.name = 'Universal Healthcare Clinic'
        org.npi = '11111111'
        render(Page::Organization::ShowComponent.new(org))
      end
    end

    # Mocks the Organization class
    class MockOrg
      attr_accessor :name, :npi, :api_id

      def initialize(row_count)
        @row_count = row_count
        @api_id = SecureRandom.uuid
      end

      def client_tokens
        tokens = []
        @row_count.times do |index|
          tokens << { 'label' => "Token #{index + 1}",
                      'id' => SecureRandom.uuid,
                      'expiresAt' => index.days.from_now.to_fs(:iso8601),
                      'createdAt' => index.days.ago.to_fs(:iso8601) }
        end
        tokens
      end

      def public_keys
        tokens = []
        @row_count.times do |index|
          tokens << { 'label' => "Key #{index + 1}",
                      'id' => SecureRandom.uuid,
                      'createdAt' => index.days.ago.to_fs(:iso8601) }
        end
        tokens
      end

      def ip_addresses
        tokens = []
        @row_count.times do |index|
          tokens << { 'label' => "IP Addr #{index + 1}",
                      'id' => SecureRandom.uuid,
                      'ip_addr' => "127.0.0.#{index + 10}",
                      'createdAt' => index.days.ago.to_fs(:iso8601) }
        end
        tokens
      end
    end
  end
end
