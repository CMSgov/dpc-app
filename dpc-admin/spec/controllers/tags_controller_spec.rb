# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe TagsController, type: :controller do
  describe '#index' do
    it_behaves_like 'an internal user authenticable controller action', :get, :index
  end

  describe '#create' do
    let!(:internal_user) { create(:internal_user) }

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      context 'valid params' do
        it 'creates a Tag' do
          expect do
            post :create, params: { tag: { name: 'Live' } }
          end.to change(Tag, :count).by(1)
        end

        it 'redirects to tags path' do
          post :create, params: { tag: { name: 'Live' } }
          expect(response.location).to include(tags_path)
        end
      end

      context 'invalid params' do
        it 'does not create a Tag' do
          expect do
            post :create, params: { tag: { name: '' } }
          end.to change(Tagging, :count).by(0)
        end

        it 'redirects to tags path' do
          post :create, params: { tag: { name: '' } }
          expect(response.location).to include(tags_path)
        end
      end
    end

    context 'unauthenticated internal user' do
      before(:each) do
        logout(:internal_user)
      end

      it 'does not create a Tag' do
        expect do
          post :create, params: { tag: { name: 'Live' } }
        end.to change(Tagging, :count).by(0)
      end

      it 'redirects to new session path' do
        post :create, params: { tag: { name: 'Live' } }
        expect(response.location).to include(new_internal_user_session_path)
      end
    end
  end

  describe '#destroy' do
    let!(:internal_user) { create(:internal_user) }

    it_behaves_like 'an internal user authenticable controller action', :delete, :destroy, :tag

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      it 'destroys the tag' do
        tag = create(:tag)
        expect { delete :destroy, params: { id: tag.id } }.to change(Tag, :count).by(-1)
      end
    end
  end
end
