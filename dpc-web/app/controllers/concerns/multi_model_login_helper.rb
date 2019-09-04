# frozen_string_literal: true

module MultiModelLoginHelper
  extend ActiveSupport::Concern
  included do
    before_action :check_user
  end

  protected

  def check_user
    if current_internal_user
      redirect_to(authenticated_internal_root_path) && return
    elsif current_user
      redirect_to(authenticated_root_path) && return
    end
  end
end
