# frozen_string_literal: true

module Page
  module Organization
    # Previews Invite Credential Delegate Success Page
    class NewOrganizationSuccessComponentPreview < ViewComponent::Preview
      def default
        render(Page::Organization::NewOrganizationSuccessComponent.new(MockOrg.new('Health Hut')))
      end
    end

    # Mocks dpc-api organization
    class MockOrg
      attr_accessor :name, :path_id

      def initialize(name)
        @name = name
        @path_id = 'some-guid'
      end
    end
  end
end
