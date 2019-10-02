# frozen_string_literal: true

module OrganizationsHelper
  def organization_types_for_select
    Organization.organization_types.keys.map do |key|
      [key.to_s.titleize, key]
    end
  end
end
