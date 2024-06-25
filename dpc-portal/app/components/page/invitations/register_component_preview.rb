# frozen_string_literal: true

module Page
  module Invitations
    # Preview page for registering
    class RegisterComponentPreview < ViewComponent::Preview
      def default
        render(Page::Invitations::RegisterComponent.new)
      end
    end
  end
end
