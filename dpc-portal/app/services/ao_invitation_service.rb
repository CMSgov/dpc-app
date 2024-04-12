# frozen_string_literal: true

# A service that verifies generates an ao invitation
class AoInvitationService
  def create_invitation(ao_given_name, ao_family_name, ao_email, org_npi)
    organization = ProviderOrganization.find_or_create_by(npi: org_npi) do |org|
      org.name = org_name(org_npi)
    end
    Invitation.create(invited_given_name: ao_given_name,
                      invited_family_name: ao_family_name,
                      invited_email: ao_email,
                      invited_email_confirmation: ao_email,
                      provider_organization: organization,
                      invitation_type: 'authorized_official')
  end

  def org_name(npi)
    org_info = client.org_info(npi)
    raise AoInvitationServiceError, 'No such organization' if org_info['code'].to_s == '404'

    org_info.dig('provider', 'orgName')
  end

  private

  def client
    @client ||= CpiApiGatewayClient.new
  end
end

class AoInvitationServiceError < StandardError; end
