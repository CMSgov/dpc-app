# frozen_string_literal: true

# Base user class
class User < ApplicationRecord
  audited only: %i[verification_reason verification_status], on: :update

  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }
  validates :verification_status, allow_nil: true,
                                  inclusion: { in: :verification_status }

  has_many :ao_org_links
  has_many :cd_org_links

  enum :verification_reason, %i[ao_med_sanction_waived ao_med_sanctions]
  enum :verification_status, %i[approved rejected]

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
end
