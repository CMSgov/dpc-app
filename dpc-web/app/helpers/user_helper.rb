module UserHelper
  def city_state(user)
    user.city + ", " + user.state
  end
end
