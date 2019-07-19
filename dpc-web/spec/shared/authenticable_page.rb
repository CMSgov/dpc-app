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

  # context 'an authenticated user' do
  #   before(:each) do
  #     logout(:user)

  #     @resource_path = path
  #     @resource_path += "/#{create(resource).id}" if resource.present?
  #     @user = User.last || create(:user)

  #     login_as(@user, scope: :user)

  #     VCR.use_cassette("shared/authenticable_page#{@resource_path.tr('/', '_')}") do
  #       visit @resource_path
  #     end
  #   end

  #   scenario 'is directed to the page' do
  #     expect(page).to have_http_status(:success)
  #     expect(page).to have_current_path(@resource_path)
  #   end

  #   scenario 'sees the navbar' do
  #     expect(page).to have_selector('#navbarSupportedContent')
  #   end

  #   scenario 'sees user name on the navbar' do
  #     expect(page.find_by_id('navbarSupportedContent')).to have_content(@user.name)
  #   end
  # end
end
