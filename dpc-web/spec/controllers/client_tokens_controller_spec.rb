require 'rails_helper'

RSpec.describe ClientTokensController, type: :controller do

  describe "GET #new" do
    let!(:user) { create(:user) }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      it "returns http success" do
        get :new
        expect(response).to have_http_status(:success)
      end
    end
  end

  describe "GET #create" do
    let!(:user) { create(:user) }

    context 'authenticated user' do
      before(:each) do
        sign_in user, scope: :user
      end

      context 'successful API request' do
        # Stub the API request

        it "returns 200 with the token params" do
          post :create
          expect(response).to have_http_status(:success)
          # expect params to be set
        end
      end

      context 'unsuccessful API request' do
        # Stub the API request

        it "returns error message" do
          post :create
          # expect response and message
          # redirects to back?
        end
      end
    end
  end

end
