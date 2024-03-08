# frozen_string_literal: true

# Link between authorized official and provider organization
class AoOrgLink < ApplicationRecord
  attr_reader :user_id, :provider_organization_id

  validates :user_id,
            uniqueness: { scope: :provider_organization_id, message: 'User already exists for this provider.' }

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true

  def show_attributes
    { user_id:, provider_organization_id: }.with_indifferent_access
  end
end
