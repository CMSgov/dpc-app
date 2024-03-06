# frozen_string_literal: true

require './lib/redis_store/mail_throttle_store'

class OrganizationUserAssignment < ApplicationRecord
  belongs_to :organization
  belongs_to :user

  validates :organization_id, presence: true
  validates :user_id, presence: true, uniqueness: { scope: :organization_id }

  after_create :send_organization_sandbox_email

  def send_organization_sandbox_email
    return unless organization.prod_sbx? && organization.registered_organization.present?

    mail_throttle_store = RedisStore::MailThrottleStore.new

    if mail_throttle_store.can_email? user.email
      UserMailer
        .with(user:, vendor: organization.health_it_vendor?)
        .organization_sandbox_email
        .deliver_later
    end
  end
end
