# frozen_string_literal: true

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    @add_path = add_provider_orgs_url
  end

  def add
    @npi = provider_org_param[:npi]

    api_request = api_service.create_provider_org(imp_id, @npi)

    if api_request.response_successful?
      flash[:notice] = 'Provider Organization added.'
      redirect_to root_path
    else
      flash[:alert] = 'Unable to add Provider Organization.'
      throw(:abort)
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
end
