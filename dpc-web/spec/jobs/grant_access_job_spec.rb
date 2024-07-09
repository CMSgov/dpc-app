# frozen_string_literal: true

require 'rails_helper'

RSpec.describe GrantAccessJob, type: :job do
  include DpcClientSupport

  describe 'new organization' do
    let(:user) { create(:user) }
    context :success do
      before { stub_api_client(message: :create_organization, success: true, response: default_org_creation_response) }
      it 'should create organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { Organization.count }.by 1
      end
      it 'should create registered organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { RegisteredOrganization.count }.by 1
      end
      it 'should bind user to organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { user.organizations.count }.by 1
      end
    end
    context :failure do
      it 'should fail'
    end
  end
  describe 'existing organization' do
    let(:user) { create(:user) }

    context :success do
      before do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        create(:organization, name: user.requested_organization)
      end
      it 'should not create organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { Organization.count }.by 0
      end
      it 'should not create registered organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { RegisteredOrganization.count }.by 1
      end
      it 'should bind user to organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { user.organizations.count }.by 1
      end
    end
  end
  describe 'existing registered organization' do
    let(:user) { create(:user) }

    context :success do
      before do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        create(:organization, :api_enabled, name: user.requested_organization)
      end
      it 'should not create organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { Organization.count }.by 0
      end
      it 'should not create registered organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { RegisteredOrganization.count }.by 0
      end
      it 'should bind user to organization' do
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { user.organizations.count }.by 1
      end
    end
  end
end
