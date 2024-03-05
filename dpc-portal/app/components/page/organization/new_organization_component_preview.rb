# frozen_string_literal: true

module Page
    module Organization
      # Preview of new organization component
      class NewOrganizationComponentPreview < ViewComponent::Preview
        #
        def default
          render(Page::Organization::NewOrganizationComponent.new)
        end
      end
    end
  end
  