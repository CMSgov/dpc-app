# frozen_string_literal: true

# Base user class
class User < ApplicationRecord
  audited only: %i[verification_reason verification_status], on: :update

  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }
  validates :verification_status, allow_nil: true,
                                  inclusion: { in: :verification_status }

  has_many :csp_users
  has_many :csps, through: :csp_users
  has_many :user_emails, through: :csp_users
  has_many :ao_org_links
  has_many :cd_org_links

  enum :verification_reason, %i[ao_med_sanction_waived ao_med_sanctions]
  enum :verification_status, %i[approved rejected]

  def csp_user_for(name)
    csp_users.joins(:csp).where(csps: { name: name }).first
  end

  def self.find_by_csp_uid(name:, csp_uid:)
    id_to_find = csp_uid
    joins(csp_users: :csp).where(csp_users: { uuid: id_to_find }, csps: { name: name }).first
  end

  def self.remember_for
    12.hours
  end

  def self.timeout_in
    30.minutes
  end

  def timeout_in
    self.class.timeout_in
  end

  def provider_links
    ao_org_links.includes(:provider_organization) +
      cd_org_links.where(disabled_at: nil).includes(:provider_organization)
  end

  def can_access?(organization)
    cd?(organization) || ao?(organization)
  end

  def ao?(organization)
    AoOrgLink.where(user: self, provider_organization: organization).exists?
  end

  def cd?(organization)
    CdOrgLink.where(user: self, provider_organization: organization, disabled_at: nil).exists?
  end

  # Returns the primary email across all CSPs
  def email
    user_emails.find_by(primary: true)&.email ||
      user_emails.first&.email
  end

  # Returns all emails across all CSPs
  def all_emails
    user_emails.map(&:email)
  end

  def self.find_by_email_in_user_emails(email)
    joins(csp_users: :user_emails)
      .where(user_emails: { email: })
      .distinct
  end
end
