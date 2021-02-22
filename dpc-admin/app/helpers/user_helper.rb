# frozen_string_literal: true

module UserHelper
  def city_state(user)
    user.city + ', ' + user.state
  end
end
