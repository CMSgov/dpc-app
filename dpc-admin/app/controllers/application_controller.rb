# frozen_string_literal: true

class ApplicationController < ActionController::Base
  before_action :no_store

  def model_error_string(resource)
    resource.errors.full_messages.join(', ')
  end

  private

  def id_param
    params.require(:id)
  end

  def sandbox?
    ENV['ENV'] == 'sandbox'
  end
  helper_method :sandbox?
end
