# frozen_string_literal: true

module Core
  module SiteHeader
    # Site Header Component
    # ----------------
    #
    class Preview < ViewComponent::Preview
      # @param logged_in toggle "Indicate whether user is logged in"
      def default(logged_in: false)
        render Core::SiteHeader::Component.new(logged_in:)
      end
    end
  end
end
