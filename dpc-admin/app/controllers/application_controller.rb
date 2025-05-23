# frozen_string_literal: true

class ApplicationController < ActionController::Base
  def model_error_string(resource)
    resource.errors.full_messages.join(', ')
  end

  private

  def id_param
    params.require(:id)
  end

  def prod_sbx?
    ENV['ENV'] == 'prod-sbx' || ENV['ENV'] == 'sandbox'
  end
  helper_method :prod_sbx?
end
