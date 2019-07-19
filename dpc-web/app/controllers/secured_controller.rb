# frozen_string_literal: true

class SecuredController < ApplicationController
  before_action :authenticate_user!
end
