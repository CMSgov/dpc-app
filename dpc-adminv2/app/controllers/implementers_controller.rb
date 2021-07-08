# frozen_string_literal: true

class ImplementersController < ApplicationController
  before_action :authenticate_admin!

  def index
  end
end
