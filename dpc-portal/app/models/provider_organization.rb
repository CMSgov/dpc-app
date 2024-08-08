# frozen_string_literal: true

# Link class to dpc-api Organization
class ProviderOrganization < ApplicationRecord
  audited only: %i[verification_reason verification_status], on: :update

  validates :npi, presence: true
  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }
  validates :verification_status, allow_nil: true,
                                  inclusion: { in: :verification_status }

  enum verification_reason: %i[org_med_sanction_waived ao_med_sanctions no_approved_enrollment org_med_sanctions]
  enum verification_status: %i[approved rejected]

  belongs_to :terms_of_service_accepted_by, class_name: 'User', required: false

  has_many :ao_org_links
  has_many :cd_org_links

  after_update :disable_rejected

  after_create do
    SyncOrganizationJob.perform_later(id) unless dpc_api_organization_id.present?
  end

  def public_keys
    @keys ||= []
    if dpc_api_organization_id.present?
      pkm = PublicKeyManager.new(dpc_api_organization_id)
      @keys = pkm.public_keys
    end
    @keys
  end

  def client_tokens
    @tokens ||= []
    if dpc_api_organization_id.present?
      ctm = ClientTokenManager.new(dpc_api_organization_id)
      @tokens = ctm.client_tokens
    end
    @tokens
  end

  def public_ips
    @ips ||= []
    if dpc_api_organization_id.present?
      ipm = IpAddressManager.new(dpc_api_organization_id)
      @ips = ipm.ip_addresses
    end
    @ips
  end

  def api_id
    id
  end

  def path_id
    id
  end

  private

  def disable_rejected
    return unless verification_status_previously_changed?(from: :approved, to: :rejected) &&
                  dpc_api_organization_id.present?

    ctm = ClientTokenManager.new(dpc_api_organization_id)
    ctm.client_tokens.each do |token|
      ctm.delete_client_token(token.with_indifferent_access)
      next if CredentialAuditLog.create(credential_type: :client_token,
                                        dpc_api_credential_id: token['id'],
                                        action: :remove)

      logger.error(['CredentialAuditLog failure',
                    { action: :remove, credential_type: :client_token, dpc_api_credential_id: token['id'] }])
    end.present? && log_disabled
  end

  def log_disabled
    logger.info(['Org API disabled',
                 { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                   actionType: LoggingConstants::ActionType::ApiBlocked,
                   providerOrganization: id }])
  end
end
