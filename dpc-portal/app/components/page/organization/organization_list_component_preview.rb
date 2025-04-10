# frozen_string_literal: true

module Page
  module Organization
    # OrganizationList
    # ----------------
    #
    # [See at USWDS]
    # https://designsystem.digital.gov/components/combo-box/
    # https://designsystem.digital.gov/components/card/
    #
    class OrganizationListComponentPreview < ViewComponent::Preview
      OrgStruct = Struct.new(:name, :npi, :api_id)

      def multiple_orgs
        links = []
        org = ProviderOrganization.new(name: 'Test Organization 0 - needs ToS', npi: '0111111111', id: 0,
                                       terms_of_service_accepted_at: nil)
        links << AoOrgLink.new(provider_organization: org)
        org = ProviderOrganization.new(name: 'Test Organization 1 - configuration needed', npi: '1111111111', id: 1,
                                       terms_of_service_accepted_at: 2.days.ago)
        links << CdOrgLink.new(provider_organization: org)
        org = ProviderOrganization.new(name: 'Test Organization 2 - configuration needed', npi: '2111111111', id: 2,
                                       terms_of_service_accepted_at: 2.days.ago, config_complete: true)
        links << AoOrgLink.new(provider_organization: org)

        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :ao, links:))
      end

      def one_org
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2,
                                       terms_of_service_accepted_at: 2.days.ago)
        link = CdOrgLink.new(provider_organization: org)
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, links: [link]))
      end

      def no_org
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, links: []))
      end
    end
  end
end
