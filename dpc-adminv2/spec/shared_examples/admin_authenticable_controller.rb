# frozen_string_literal: true

RSpec.shared_examples 'an admin authenticable controller action' do |meth, action, resource, params: {}|
  context 'an unauthenticated admin' do
    before(:each) do
      logout(:admin)

      if resource
        params[:id] = create(resource).id
      end

      send meth, action, params: params
    end

    scenario 'is redirected to the sign in page' do
      expect(response.location).to include(new_admin_session_path)
    end
  end

  context 'an authenticated normal user' do
    before(:each) do
      logout(:admin)

      user = create(:user)
      sign_in user, scope: :user

      if resource
        params[:id] = create(resource).id
      end

      send meth, action, params: params
    end

    scenario 'is redirected to the home page' do
      expect(response.location).to include(new_admin_session_path)
    end
  end

  context 'an authenticated admin' do
    before(:each) do
      logout(:admin)

      if resource
        params[:id] = create(resource).id
      end

      @admin = Admin.last || create(:admin)

      sign_in @admin, scope: :admin
      send meth, action, params: params
    end

    # FIXME this should be meth, but will require some test refactoring
    if action == :get
      scenario 'renders the requested page' do
        expect(response).to render_template(action)
      end
    end
  end
end
