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

      def multiple_orgs_ao
        links = []
        3.times do |i|
          org = ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i)
          links << AoOrgLink.new(provider_organization: org)
        end
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :ao, links:))
      end

      def multiple_orgs
        orgs = []
        3.times do |i|
          org = ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i)
          links << CdOrgLink.new(provider_organization: org)
        end
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: orgs))
      end

      def one_org
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        link = CdOrgLink.new(provider_organization: org)
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: [link]))
      end

      def no_org
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: []))
      end
    end
  end
end
