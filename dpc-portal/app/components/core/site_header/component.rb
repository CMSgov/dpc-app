# frozen_string_literal: true

module Core
  module SiteHeader
    # Renders the DPC Portal site header.
    class Component < ViewComponent::Base
      attr_accessor :logged_in

      def initialize(logged_in: false)
        super
        @logged_in = logged_in
      end
    end
  end
end
