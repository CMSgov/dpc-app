class ApplicationController < ActionController::Base
  private

  def id_param
    params.require(:id)
  end
end
