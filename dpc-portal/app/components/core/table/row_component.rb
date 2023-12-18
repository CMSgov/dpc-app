# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled row
    class RowComponent < ViewComponent::Base
      with_collection_parameter :obj

      attr_accessor :attributes, :iteration

      def initialize(obj:, obj_iteration:, keys:)
        super
        @attributes = []
        @iteration = obj_iteration
        keys.each do |key|
          attributes << format_if_date(obj[key] || key)
        end
      end

      def format_if_date(str)
        datetime = DateTime.parse(str)
        datetime.strftime('%m/%d/%Y at %l:%M%p UTC')
      rescue Date::Error
        str
      end
    end
  end
end
