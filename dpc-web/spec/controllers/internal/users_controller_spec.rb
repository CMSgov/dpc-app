# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Internal::UsersController, type: :controller do
  describe '#index' do
    context 'authenticated internal user' do
      before(:each) do
        internal_user = create(:internal_user)
        sign_in internal_user, scope: :internal_user
      end

      it 'assigns @users to all users' do
        users = create_list(:user, 2)
        get :index
        expect(assigns(:users)).to eq(users.reverse)
      end

      it 'renders the index template' do
        get :index
        expect(response).to render_template('index')
      end
    end

    # TODO: Write shared examples for user and no user or internal user
    # context 'invalud authentication'
  end
end
