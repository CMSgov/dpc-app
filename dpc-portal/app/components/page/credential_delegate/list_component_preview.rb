# frozen_string_literal: true

module Page
  module CredentialDelegate
    # List of Delegates
    # ----------------
    #
    # [See at USWDS]
    # https://designsystem.digital.gov/components/combo-box/
    # https://designsystem.digital.gov/components/card/
    #
    class ListComponentPreview < ViewComponent::Preview
      def empty
        render(Page::CredentialDelegate::ListComponent.new(org, []))
      end

      def active
        cds = [
          CdOrgLink.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com', pending: false),
          CdOrgLink.new(given_name: 'Lisa', family_name: 'Franklin', email: 'lisa@example.com', pending: false)
        ]
        render(Page::CredentialDelegate::ListComponent.new(org, cds))
      end

      def pending
        cds = [
          CdOrgLink.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com', pending: true),
          CdOrgLink.new(given_name: 'Lisa', family_name: 'Franklin', email: 'lisa@example.com', pending: true)
        ]
        render(Page::CredentialDelegate::ListComponent.new(org, cds))
      end

      private

      def org
        MockOrg.new('Health Hut', '111111111')
      end
    end

    # Mocks dpc-api Organization
    class MockOrg
      attr_accessor :name, :npi, :path_id

      def initialize(name, npi)
        @name = name
        @npi = npi
        @path_id = SecureRandom.uuid
      end
    end
  end
end
