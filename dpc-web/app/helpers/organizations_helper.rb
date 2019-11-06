# frozen_string_literal: true

module OrganizationsHelper
  def organization_types_for_select
    enum_for_select(Organization.organization_types)
  end

  def address_uses_for_select
    enum_for_select(Address.address_uses)
  end

  def address_types_for_select
    enum_for_select(Address.address_types)
  end

  def statuses_for_select
    enum_for_select(FhirEndpoint.statuses)
  end

  def enum_for_select(collection)
    collection.keys.map do |key|
      [key.to_s.titleize, key]
    end
  end
end
