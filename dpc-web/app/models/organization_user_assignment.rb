# frozen_string_literal: true

class OrganizationUserAssignment < ApplicationRecord
  belongs_to :organization
  belongs_to :user

  validates :organization_id, presence: true
  validates :user_id, presence: true, uniqueness: { scope: :organization_id }
end
