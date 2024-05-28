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
          org = ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i,
                                         terms_of_service_accepted_at: 2.days.ago)
          links << AoOrgLink.new(provider_organization: org)
        end
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :ao, links:))
      end

      def multiple_orgs
        links = []
        3.times do |i|
          org = ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i,
                                         terms_of_service_accepted_at: 2.days.ago)
          links << CdOrgLink.new(provider_organization: org)
        end
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, links:))
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
