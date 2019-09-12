# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe Internal::UsersController, type: :controller do
  describe '#index' do
    let!(:internal_user) { create(:internal_user) }

    it_behaves_like "an internal user authenticable controller action", :get, :index

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      it 'assigns @users to all users' do
        users = create_list(:user, 2)
        get :index
        expect(assigns(:users)).to eq(users.reverse)
      end
    end

    # context 'invalud authentication'
  end

  describe '#show' do
    it_behaves_like "an internal user authenticable controller action", :get, :show, :user
  end
end
