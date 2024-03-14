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
        user1 = User.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com')
        user2 = User.new(given_name: 'Lisa', family_name: 'Franklin', email: 'lisa@example.com')
        cds = [CdOrgLink.new(user: user1, created_at: 1.week.ago), CdOrgLink.new(user: user2, created_at: 2.weeks.ago)]
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
        ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
      end
    end
  end
end
