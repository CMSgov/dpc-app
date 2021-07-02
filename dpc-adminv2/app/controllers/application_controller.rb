class ApplicationController < ActionController::Base
  def model_error_string(resource)
    resource.errors.full_messages.join(', ')
  end

  private

  def id_param
    params.require(:id)
  end
end
