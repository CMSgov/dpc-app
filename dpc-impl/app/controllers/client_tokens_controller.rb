# frozen_string_literal: true

class ClientTokensController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
  end

  def create
  end

  def destroy
  end

  private

  def org_id
    params[:org_id]
  end

  def params_present?
    params[:label].present?
  end
end