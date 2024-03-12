# frozen_string_literal: true

# Link between authorized official and provider organization
class AoOrgLink < ApplicationRecord
  validates :user_id,
            uniqueness: { scope: :provider_organization_id, message: 'User already exists for this provider.' }

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true

  def show_attributes
    { full_name: "#{user.given_name} #{user.family_name}",
      email: user.email.to_s }.with_indifferent_access
  end
end
