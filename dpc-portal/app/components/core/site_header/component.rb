# frozen_string_literal: true

module Core
  module SiteHeader
    # Renders the DPC Portal site header.
    class Component < ViewComponent::Base
      attr_accessor :logged_in, :border

      def initialize(logged_in: false, border: true)
        super
        @logged_in = logged_in
        @border = border
      end
    end
  end
end
