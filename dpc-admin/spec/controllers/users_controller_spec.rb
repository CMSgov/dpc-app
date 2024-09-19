# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe UsersController, type: :controller do
  describe '#index' do
    let!(:internal_user) { create(:internal_user) }

    it_behaves_like 'an internal user authenticable controller action', :get, :index

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
  end

  describe '#show' do
    it_behaves_like 'an internal user authenticable controller action', :get, :show, :user
  end

  describe '#edit' do
    it_behaves_like 'an internal user authenticable controller action', :get, :edit, :user
  end

  describe '#update' do
    it_behaves_like 'an internal user authenticable controller action',
                    :put, :update, :user, params: { user: { first_name: 'Riley' } }
  end

  describe '#download' do
    let!(:internal_user) { create(:internal_user) }

    before(:each) do
      sign_in internal_user, scope: :internal_user
    end

    it 'sends file from User.to_csv' do
      allow(User).to receive(:to_csv).and_return('test_file')

      get :download, format: :csv, params: { users: [50000] }

      expect(response.body).to eq('test_file')
    end

    it 'redirects with error if no users' do
      get :download, format: :csv

      expect(response).to redirect_to root_path
    end
  end
end
