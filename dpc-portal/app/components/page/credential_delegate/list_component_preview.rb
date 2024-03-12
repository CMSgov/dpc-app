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
          CdOrgLink.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com', pending: false),
          CdOrgLink.new(given_name: 'Lisa', family_name: 'Franklin', email: 'lisa@example.com', pending: false)
        ]
        render(Page::CredentialDelegate::ListComponent.new(org, [], cds))
      end

      def pending
        cds = [
          Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com',
                         verification_code: 'ABC123'),
          Invitation.new(invited_given_name: 'Lisa', invited_family_name: 'Franklin',
                         invited_email: 'lisa@example.com', verification_code: '123ABC')
        ]
        render(Page::CredentialDelegate::ListComponent.new(org, cds, []))
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
