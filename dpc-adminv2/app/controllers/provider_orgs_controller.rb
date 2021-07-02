# frozen_string_literal: true

class ProviderOrgsController < ApplicationController
  before_action :authenticate_admin!

  def index
  end
end
