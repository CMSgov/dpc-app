# frozen_string_literal: true

module RegistrationsHelper
  def organization_types_for_select
    User.organization_types.keys.each_with_object([]) { |key, memo| memo << [key.titleize, key] }
  end
end
