# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe TaggingsController, type: :controller do
  describe '#create' do
    let!(:internal_user) { create(:internal_user) }
    let(:tag) { create(:tag) }
    let(:user) { create(:user) }

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      context 'valid params' do
        it 'creates a Tagging' do
          expect do
            post :create, params: { tagging: { tag_id: tag.id, taggable_id: user.id, taggable_type: 'User' } }
          end.to change(Tagging, :count).by(1)
        end

        it 'redirects to user path' do
          post :create, params: { tagging: { tag_id: tag.id, taggable_id: user.id, taggable_type: 'User' } }
          expect(response.location).to include(user_path(id: user.id))
        end
      end

      context 'invalid params' do
        it 'does not create a Tagging' do
          expect do
            post :create, params: { tagging: { tag_id: nil, taggable_id: user.id, taggable_type: 'User' } }
          end.to change(Tagging, :count).by(0)
        end

        it 'redirects to user path' do
          post :create, params: { tagging: { tag_id: nil, taggable_id: user.id, taggable_type: 'User' } }
          expect(response.location).to include(user_path(id: user.id))
        end
      end
    end

    context 'unauthenticated internal user' do
      before(:each) do
        logout(:internal_user)
      end

      it 'does not create a Tagging' do
        expect do
          post :create, params: { tagging: { tag_id: tag.id, taggable_id: user.id, taggable_type: 'User' } }
        end.to change(Tagging, :count).by(0)
      end

      it 'redirects to new session path' do
        post :create, params: { tagging: { tag_id: tag.id, taggable_id: user.id, taggable_type: 'User' } }
        expect(response.location).to include(new_internal_user_session_path)
      end
    end
  end

  describe '#destroy' do
    let!(:internal_user) { create(:internal_user) }

    it_behaves_like 'an internal user authenticable controller action', :delete, :destroy, :tagging

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      it 'destroys the tagging' do
        tagging = create(:tagging)
        expect { delete :destroy, params: { id: tagging.id } }.to change(Tagging, :count).by(-1)
      end
    end
  end
end
