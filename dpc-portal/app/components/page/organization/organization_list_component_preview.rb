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
        orgs = []
        3.times { |i| orgs << ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i) }
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :ao, organizations: orgs))
      end

      def multiple_orgs
        orgs = []
        3.times { |i| orgs << ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i) }
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: orgs))
      end

      def one_org
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: [org]))
      end

      def no_org
        render(Page::Organization::OrganizationListComponent.new(ao_or_cd: :cd, organizations: []))
      end
    end
  end
end
