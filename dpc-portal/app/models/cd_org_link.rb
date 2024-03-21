# frozen_string_literal: true

# Link between credential delegate and provider organization
class CdOrgLink < ApplicationRecord
  belongs_to :user, required: true
  belongs_to :provider_organization, required: true
  belongs_to :invitation, required: true

  def show_attributes
    { full_name: "#{user.given_name} #{user.family_name}",
      email: user.email,
      verification_code: invitation&.verification_code,
      activated_at: created_at.to_s }.with_indifferent_access
  end
end
