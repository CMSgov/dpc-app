# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  def show
    render plain: "Hello, org #{params[:id]}"
  end
end
