module MultiModelLoginHelper
  extend ActiveSupport::Concern
  included do
    before_action :check_user
  end

  protected
  def check_user
    # binding.pry
    if current_internal_user
      flash.clear
      redirect_to(authenticated_internal_root_path) && return
    elsif current_user
      flash.clear
      # The authenticated root path can be defined in your routes.rb in: devise_scope :user do...
      redirect_to(authenticated_root_path) && return
    end
  end
end