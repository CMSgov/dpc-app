# frozen_string_literal: true

class OrganizationUserAssignment < ApplicationRecord
  belongs_to :organization
  belongs_to :user

  validates :organization_id, presence: true
  validates :user_id, presence: true, uniqueness: { scope: :organization_id }

  after_create :send_organization_sandbox_email

  def send_organization_sandbox_email
    return unless organization.sandbox_enabled?

    UserMailer.with(user: user, organization: organization).organization_sandbox_email.deliver_later
  end
end
