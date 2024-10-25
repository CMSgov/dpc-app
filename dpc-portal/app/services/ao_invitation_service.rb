# frozen_string_literal: true

# A service that verifies generates an ao invitation
class AoInvitationService
  def create_invitation(given_name, family_name, ao_email, org_npi)
    organization = ProviderOrganization.find_or_create_by(npi: org_npi) do |org|
      org.name = org_name(org_npi)
    end

    invitation = Invitation.create(invited_email: ao_email,
                                   invited_email_confirmation: ao_email,
                                   provider_organization: organization,
                                   invitation_type: :authorized_official)

    Rails.logger.info(['Authorized Official invited',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoInvited }])

    InvitationMailer.with(invitation:, given_name:, family_name:).invite_ao.deliver_now

    invitation
  end

  def org_name(npi)
    raise AoInvitationServiceError, 'Bad NPI' unless Luhnacy.valid?("80840#{npi}")

    org_info = client.org_info(npi)
    raise AoInvitationServiceError, "No organization with npi: #{npi}" if org_info['code'].to_s == '404'

    org_info.dig('provider', 'orgName')
  end

  private

  def client
    @client ||= CpiApiGatewayClient.new
  end
end

class AoInvitationServiceError < StandardError; end
