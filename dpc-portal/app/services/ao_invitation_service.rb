# frozen_string_literal: true

# A service that verifies generates an ao invitation
class AoInvitationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  def org_name(npi); end
end
