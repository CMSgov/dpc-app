# frozen_string_literal: true

# Handles entry
class MainController < ApplicationController
  def welcome
    render plain: 'Hello, World'
  end
end
