# frozen_string_literal: true

class ProviderOrgsController < ApplicationController
  before_action :authenticate_user!

  def show
    @user = current_user
    @add_path = add_provider_orgs_url
  end

  def add
    binding.pry
  end

  private

  def provider_org_params
    params.require(:provider_org).permit(:npi)
  end
end
