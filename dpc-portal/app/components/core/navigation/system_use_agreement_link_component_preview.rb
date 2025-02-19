# frozen_string_literal: true

module Core
  module Navigation
    # SystemUseAgreementComponent preview
    class SystemUseAgreementLinkComponentPreview < ViewComponent::Preview
      def default
        render(Core::Navigation::SystemUseAgreementLinkComponent.new)
      end
    end
  end
end
