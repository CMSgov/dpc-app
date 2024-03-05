# frozen_string_literal: true

RSpec.shared_examples 'an internal user authenticable controller action' do |meth, action, resource, params: {}|
  context 'an unauthenticated internal user' do
    before(:each) do
      logout(:internal_user)

      if resource
        params[:id] = create(resource).id
      end

      send meth, action, params:
    end

    scenario 'is redirected to the sign in page' do
      expect(response.location).to include(new_internal_user_session_path)
    end
  end

  context 'an authenticated normal user' do
    before(:each) do
      logout(:internal_user)

      user = create(:user)
      sign_in user, scope: :user

      if resource
        params[:id] = create(resource).id
      end

      send meth, action, params:
    end

    scenario 'is redirected to the home page' do
      expect(response.location).to include(new_internal_user_session_path)
    end
  end

  context 'an authenticated internal user' do
    before(:each) do
      logout(:internal_user)

      if resource
        params[:id] = create(resource).id
      end

      @internal_user = InternalUser.last || create(:internal_user)

      sign_in @internal_user, scope: :internal_user
      send meth, action, params:
    end

    # FIXME this should be meth, but will require some test refactoring
    if action == :get
      scenario 'renders the requested page' do
        expect(response).to render_template(action)
      end
    end
  end
end
