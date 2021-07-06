# frozen_string_literal: true

require './lib/luhnacy_lib/luhnacy_lib'

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def new
    @user = current_user
    @npi = generate_npi
  end

  def add
    @npi = provider_org_param[:npi]

    api_request = api_service.create_provider_org(imp_id, @npi)

    return npi_error unless npi_valid?(@npi)

    if api_request.response_successful?
      flash[:notice] = 'Provider Organization added.'
      redirect_to root_path
    else
      binding.pry
      msg = api_request.response_body
      flash[:alert] = "Provider Organization could not be added: #{msg}"
      redirect_to new_provider_orgs_path
    end
  end

  private

  def api_service
    @api_service ||= ApiClient.new
  end

  def imp_id
    current_user.implementer_id
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
end
