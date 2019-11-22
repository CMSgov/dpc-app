# frozen_string_literal: true

require 'rails_helper'

require './spec/shared_examples/internal_user_authenticable_controller'

RSpec.describe Internal::OrganizationsController, type: :controller do
  describe '#index' do
    let!(:internal_user) { create(:internal_user) }

    it_behaves_like 'an internal user authenticable controller action', :get, :index

    context 'authenticated internal user' do
      before(:each) do
        sign_in internal_user, scope: :internal_user
      end

      it 'assigns @organizations to all organizations' do
        organizations = create_list(:organization, 2)
        get :index
        expect(assigns(:organizations)).to eq(organizations)
      end
    end
  end

  describe '#show' do
    it_behaves_like 'an internal user authenticable controller action', :get, :show, :organization
  end

  describe '#edit' do
    it_behaves_like 'an internal user authenticable controller action', :get, :edit, :organization
  end

  describe '#update' do
    it_behaves_like 'an internal user authenticable controller action',
                    :put, :update, :organization, params: { organization: { name: 'Good Health' } }

    context 'authenticated internal user' do
      let!(:internal_user) { create(:internal_user) }
      let!(:organization) { create(:organization, api_environments: []) }
      let!(:assignment) { create(:organization_user_assignment, organization: organization) }
      let!(:mailer) { double(UserMailer) }

      before(:each) do
        allow(UserMailer).to receive(:with).and_return(mailer)
        allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
        allow(mailer).to receive(:deliver_later)

        sign_in internal_user, scope: :internal_user
      end

      it 'does not kick off notification if sandbox is not added' do
        patch :update, params: { id: organization.id, organization: { api_environments: ['1']} }
        expect(UserMailer).not_to have_received(:with)
      end

      it 'kicks off notification if sandbox is added' do
        patch :update, params: { id: organization.id, organization: { api_environments: ['0']} }

        expect(UserMailer).to have_received(:with)
          .once.with(user: assignment.user, organization: organization)
        expect(mailer).to have_received(:organization_sandbox_email)
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

      it 'destroys the organization' do
        org = create(:organization)
        expect { delete :destroy, params: { id: org.id } }.to change(Organization, :count).by(-1)
      end
    end
  end
end
