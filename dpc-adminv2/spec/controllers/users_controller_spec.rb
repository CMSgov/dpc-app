# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/admin_authenticable_controller'

RSpec.describe UsersController, type: :controller do
  describe '#index' do
    let!(:admin) { create(:admin) }

    it_behaves_like 'an admin authenticable controller action', :get, :index

    context 'authenticated admin' do
      before(:each) do
        sign_in admin
      end

      it 'assigns @users to all users' do
        users = create_list(:user, 2)
        get :index
        expect(assigns(:users)).to eq(users.reverse)
      end
    end
  end

  describe '#show' do
    it_behaves_like 'an admin authenticable controller action', :get, :show, :user
  end

  describe '#update' do
    it_behaves_like 'an admin authenticable controller action', :put, :update, :user, params: { user: { first_name: 'Susan' }}
  end

  describe '#destroy' do
    let!(:admin) { create(:admin) }

    it_behaves_like 'an admin authenticable controller action', :delete, :destroy, :user

    context 'authenticated admin' do
      before(:each) do
        sign_in admin
      end

      it 'destroys user' do
        user = create(:user)
        expect { delete :destroy, params: { id: user.id }}.to change(User, :count).by(-1)
      end
    end
  end
end
