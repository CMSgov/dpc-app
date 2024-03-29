# frozen_string_literal: true

module Core
  module SiteHeader
    # Site Header Component
    # ----------------
    #
    class Preview < ViewComponent::Preview
      def default
        render Core::SiteHeader::Component.new
      end
    end
  end
end
