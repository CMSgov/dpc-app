# frozen_string_literal: true

RSpec.shared_examples 'an authenticable page' do |path, resource|
  context 'an unauthenticated user' do
    before(:each) do
      logout(:user)
      if resource.present?
        visit path + "/#{create(resource).id}"
      else
        visit path
      end
    end

    scenario 'is redirected to the sign in page' do
      expect(page).to have_current_path(user_session_path)
    end
  end

  context 'an authenticated user' do
    before(:each) do
      logout(:user)

      @resource_path = path
      @resource_path += "/#{create(resource).id}" if resource.present?
      @user = User.last || create(:user)

      login_as(@user, scope: :user)
      visit @resource_path
    end

    scenario 'is directed to the page' do
      expect(page).to have_http_status(:success)
      expect(page).to have_current_path(@resource_path)
    end
  end
end
