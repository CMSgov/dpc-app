# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CurrentAttributes do
  before(:each) do
    CurrentAttributes.reset
  end

  describe :save_user_attributes do
    it 'does nothing when user is nil' do
      CurrentAttributes.save_user_attributes(nil)
      expect(CurrentAttributes.current_user).to eq(nil)
    end

    it 'records user attributes when user exists' do
      user = build(:user, id: rand(0..100_000), pac_id: rand(0..100_000).to_s)
      CurrentAttributes.save_user_attributes(user)
      expect(CurrentAttributes.current_user).to eq(
        {
          id: user.id,
          external_id: user.uid,
          pac_id: user.pac_id
        }
      )
    end
  end

  describe :save_organization_attributes do
    it 'does nothing when organization is nil' do
      CurrentAttributes.save_organization_attributes(nil, nil)
      expect(CurrentAttributes.organization).to eq(nil)
    end

    it 'records organization attributes when user is nil' do
      org = build(:provider_organization, id: rand(0..100_000), dpc_api_organization_id: rand(0..100_000).to_s)
      CurrentAttributes.save_organization_attributes(org, nil)
      expect(CurrentAttributes.organization).to eq(
        {
          id: org.id,
          dpc_api_organization_id: org.dpc_api_organization_id
        }
      )
    end

    it 'records role attributes when user exists' do
      user = create(:user, id: rand(0..100_000), pac_id: rand(0..100_000).to_s)
      org = create(:provider_organization, id: rand(0..100_000), dpc_api_organization_id: rand(0..100_000).to_s)
      create(:ao_org_link, user:, provider_organization: org)

      CurrentAttributes.save_organization_attributes(org, user)
      expect(CurrentAttributes.organization).to eq(
        {
          id: org.id,
          dpc_api_organization_id: org.dpc_api_organization_id,
          is_authorized_official: true,
          is_credential_delegate: false
        }
      )
    end
  end

  describe :to_log_hash do
    it 'records all attributes' do
      CurrentAttributes.request_id = 1234
      CurrentAttributes.request_user_agent = 'Chrome'
      CurrentAttributes.request_ip = '127.0.0.1'
      CurrentAttributes.forwarded_for = '127.0.0.2'
      CurrentAttributes.method = 'POST'
      CurrentAttributes.path = 'my/path'
      CurrentAttributes.current_user = {
        id: 1234,
        external_id: '2345',
        pac_id: '3456'
      }
      CurrentAttributes.organization = {
        id: 987,
        dpc_api_organization_id: '876-45',
        is_authorized_official: true,
        is_credential_delegate: false
      }

      expect(CurrentAttributes.to_log_hash).to eq(
        {
          request_id: 1234,
          request_user_agent: 'Chrome',
          request_ip: '127.0.0.1',
          forwarded_for: '127.0.0.2',
          method: 'POST',
          path: 'my/path',
          current_user: {
            id: 1234,
            external_id: '2345',
            pac_id: '3456'
          },
          organization: {
            id: 987,
            dpc_api_organization_id: '876-45',
            is_authorized_official: true,
            is_credential_delegate: false
          }
        }
      )
    end
  end
end
