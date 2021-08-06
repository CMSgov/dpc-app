# frozen_string_literal: true

require './lib/luhnacy_lib/luhnacy_lib'

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def new
    @user = current_user
    @npi = generate_npi
  end

  def show
    @org_id = params[:org_id]

    org_api_request = api_service.get_organization(@org_id)

    if org_api_request[:id].present? && org_api_request[:id] == @org_id
      @org = org_api_request
      @npi = org_npi(@org[:identifier])
      @status = org_status(@npi)

      @client_tokens = get_client_tokens(imp_id, @org_id)
      @public_keys = get_public_keys(imp_id, @org_id)
    else
      flash[:alert] = "We were unable to connect to DPC due to an internal error. Please try again at a later time."
      redirect_to portal_path
    end
  end

  def add
    @npi = provider_org_param[:npi]

    api_request = api_service.create_provider_org(imp_id, @npi)

    return npi_error unless npi_valid?(@npi)

    if api_request.response_successful?
      flash[:notice] = 'Provider Organization added.'
      redirect_to root_path
    else
      msg = api_request.response_body
      flash[:alert] = "Provider Organization could not be added: #{msg}"
      redirect_to new_provider_orgs_path
    end
  end

  private

  def get_client_tokens(imp_id, org_id)
    api_req = tokens_keys_api_req(imp_id, org_id)

    if api_req.class == Hash
      return api_req[:client_tokens]
    else
      return []
    end
  end

  def get_public_keys(imp_id, org_id)
    api_req = tokens_keys_api_req(imp_id, org_id)

    if api_req.class == Hash
      return api_req[:public_keys]
    else
      return []
    end
  end

  def org_npi(org)
    hash = org.first
    return hash[:value]
  end

  def org_status(npi)
    @npi = npi
    @orgs = current_user.provider_orgs
    org = @orgs.select { |org| org[:npi] == @npi }
    
    return org.first[:status]
  end
  
  def provider_org_param
    params.require(:provider_org).permit(:npi)
  end

  def npi_error
    flash[:alert] = 'NPI must be valid.'
    redirect_to provider_orgs_path
  end

  def npi_valid?(npi)
    npi = '80840' + npi
    LuhnacyLib.validate_npi(npi)
  end

  def tokens_keys_api_req(imp_id, org_id)
    api_service.get_tokens_keys(imp_id, org_id)
  end
end
