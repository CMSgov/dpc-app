# frozen_string_literal: true

# Handles entry
class MainController < ApplicationController
  before_action :authenticate_user!
  
  def welcome
    render plain: 'Hello, World'
  end
end
