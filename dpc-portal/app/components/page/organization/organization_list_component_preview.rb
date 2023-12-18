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
      OrgStruct = Struct.new(:name, :npi)

      def multiple_orgs
        render(Page::Organization::OrganizationListComponent.new(organizations: [
                                                     OrgStruct.new('Test Organization 1', 'npi_111111'),
                                                     OrgStruct.new('Test Organization 2', 'npi_222222'),
                                                     OrgStruct.new('Test Organization 3', 'npi_333333')
                                                   ]))
      end

      def one_org
        render(Page::Organization::OrganizationListComponent.new(organizations: [OrgStruct.new('Test Organization', 'npi_123456')]))
      end

      def no_org
        render(Page::Organization::OrganizationListComponent.new(organizations: []))
      end
    end
  end
end
