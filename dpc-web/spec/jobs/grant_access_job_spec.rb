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
          organization = GrantAccessJob.perform_now(user.id)
          expect(organization.users).to include(user)
        end.to change { OrganizationUserAssignment.count }.by 1
      end
    end
    context :failure do
      before do
        expect(Rails.logger).to receive(:error).with(/GrantAccessJob failure/)
      end
      it 'should fail if no user' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        expect do
          GrantAccessJob.perform_now(:foo)
        end.to change { Organization.count }.by 0
      end
      it 'should fail if cannot save organization' do
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)
        bad_org = Organization.new
        bad_org.errors.add(:base, 'bad')
        org_double = class_double(Organization).as_stubbed_const
        expect(org_double).to receive(:find_or_create_by!).and_raise(ActiveRecord::RecordInvalid, bad_org)
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { OrganizationUserAssignment.count }.by 0
      end
      it 'should fail if cannot save registered organization' do
        stub_api_client(message: :create_organization, success: false)
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { RegisteredOrganization.count }.by 0
        expect(user.reload.organizations.length).to eq 0
      end
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
          organization = GrantAccessJob.perform_now(user.id)
          expect(organization.users).to include(user)
        end.to change { user.organizations.count }.by 1
      end
    end
    context :failure do
      it 'should fail if cannot register registered organization' do
        stub_api_client(message: :create_organization, success: false)
        expect do
          GrantAccessJob.perform_now(user.id)
        end.to change { RegisteredOrganization.count }.by 0
        expect(user.reload.organizations.length).to eq 0
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
        organization = GrantAccessJob.perform_now(user.id)
        expect(organization.users).to include(user)
      end
    end
  end
end
