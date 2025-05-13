# frozen_string_literal: true

# Link between authorized official and provider organization
class AoOrgLink < ApplicationRecord
  audited only: %i[verification_reason verification_status], on: :update

  validates :user_id,
            uniqueness: { scope: :provider_organization_id, message: 'already exists for this provider.' }
  validates :invitation_id, allow_nil: true,
                            uniqueness: { scope: :invitation_id, message: 'already used by another AoOrgLink.' }
  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }

  enum :verification_reason, %i[user_not_authorized_official ao_med_sanctions no_approved_enrollment org_med_sanctions]

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true
  belongs_to :invitation, required: false

  def ao?
    true
  end
end
