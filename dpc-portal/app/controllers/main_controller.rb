# frozen_string_literal: true

# Handles entry
class MainController < ApplicationController
  def welcome
    render plain: 'Hello, World'
  end

  private

  def block_prod_sbx; end
end
