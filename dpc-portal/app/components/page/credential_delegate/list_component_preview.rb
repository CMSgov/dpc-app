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
        render(Page::CredentialDelegate::ListComponent.new(org, [], []))
      end

      def active
        cds = [
          CdOrgLink.new,
          CdOrgLink.new
        ]
        render(Page::CredentialDelegate::ListComponent.new(org, [], cds))
      end

      private

      def org
        MockOrg.new('Health Hut')
      end
    end

    # Mocks dpc-api Organization
    class MockOrg
      attr_accessor :name, :npi, :path_id

      def initialize(name)
        @name = name
        @npi = '11111111'
        @path_id = SecureRandom.uuid
      end
    end
  end
end
