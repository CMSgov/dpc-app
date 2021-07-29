# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = params[:org_id]
  end

  def create
    binding.pry
  end

  def index
  end
end
