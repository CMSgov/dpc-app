# frozen_string_literal: true

module Page
  module Organization
    # AoLanding
    # ----------------
    #
    # Landing page for authorized officials, which shows a list of their organizations.
    #
    class AoLandingComponentPreview < ViewComponent::Preview
      def multiple_orgs
        orgs = []
        3.times { |i| orgs << ProviderOrganization.new(name: "Test Organization #{i}", npi: "#{i}111111111", id: i) }
        render(Page::Organization::AoLandingComponent.new(organizations: orgs))
      end

      def one_org
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::Organization::AoLandingComponent.new(organizations: [org]))
      end

      def no_org
        render(Page::Organization::AoLandingComponent.new(organizations: []))
      end
    end
  end
end
