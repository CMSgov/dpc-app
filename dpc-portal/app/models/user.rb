# frozen_string_literal: true

# Base user class
class User < ApplicationRecord
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, and :trackable
  devise :database_authenticatable, :registerable,
         :recoverable, :rememberable, :validatable,
         :timeoutable, :omniauthable, omniauth_providers: [:openid_connect]

  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }
  validates :verification_status, allow_nil: true,
                                  inclusion: { in: :verification_status }

  has_many :ao_org_links
  has_many :cd_org_links

  enum :verification_reason, %i[ao_med_sanction_waived ao_med_sanctions]
  enum :verification_status, %i[approved rejected]

  before_validation(on: :create) do
    # Assign random, acceptable password to keep Devise happy.
    # User should log in only through IdP
    self.password = Devise.friendly_token[0, 20] unless password.present?
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
