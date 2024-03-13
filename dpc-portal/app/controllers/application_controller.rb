# frozen_string_literal: true

# Parent class of all controllers
class ApplicationController < ActionController::Base
  private

  def block_prod_sbx
    redirect_to root_url if ENV.fetch('ENV', nil) == 'prod-sbx'
  end
end
