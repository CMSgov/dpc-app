# frozen_string_literal: true

module UserHelper
  def invited_by(id)
    user = User.where(id: id).first
    return user.name
  end
end
