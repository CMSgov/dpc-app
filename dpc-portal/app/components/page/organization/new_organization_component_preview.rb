# frozen_string_literal: true

module Page
  module Organization
    # Preview of new organization component
    class NewOrganizationComponentPreview < ViewComponent::Preview
      def default
        render(Page::Organization::NewOrganizationComponent.new)
      end

      def blank_error
        render(Page::Organization::NewOrganizationComponent.new("Can't be blank"))
      end

      def length_error
        render(Page::Organization::NewOrganizationComponent.new('Length has to be 10'))
      end
    end
  end
end
