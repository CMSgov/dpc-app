# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  describe 'created by provider' do
    let(:user_provider) { 'logindotgov' }
    let(:user_uid) { 'user-uid' }
    let(:provider_data) { MockAuthInfo.new(user_provider, user_uid, 'user@example.com') }

    it 'creates when no records' do
      expect { User.create_from_provider_data(provider_data) }.to change { User.count }.by 1
    end

    it 'creates when provider matches but not uid' do
      User.create!(provider: user_provider,
                   uid: 'another-uid',
                   email: 'anotheruser@example.com',
                   password: Devise.friendly_token[0, 20])
      expect { User.create_from_provider_data(provider_data) }.to change { User.count }.by 1
    end
    it 'creates when uid matches but not provider' do
      User.create!(provider: 'another-provider',
                   uid: user_uid,
                   email: 'anotheruser@example.com',
                   password: Devise.friendly_token[0, 20])
      expect { User.create_from_provider_data(provider_data) }.to change { User.count }.by 1
    end
    it 'does not create when uid and provider match' do
      old_user = User.create!(provider: user_provider,
                              uid: user_uid,
                              email: 'anotheruser@example.com',
                              password: Devise.friendly_token[0, 20])
      new_user = nil
      expect { new_user = User.create_from_provider_data(provider_data) }.to change { User.count }.by 0
      expect(old_user).to eq new_user
    end
  end

  describe :ao do
    let(:user) { create(:user) }
    let(:other_user) { create(:user) }
    let(:provider_organization) { create(:provider_organization) }

    it 'should be ao if link present' do
      create(:ao_org_link, user:, provider_organization:)
      expect(user.ao?(provider_organization)).to be true
    end

    it 'should not be ao if link not present' do
      create(:ao_org_link, user: other_user, provider_organization:)
      expect(user.ao?(provider_organization)).to be false
    end
  end
  describe :cd do
    let(:user) { create(:user) }
    let(:other_user) { create(:user) }
    let(:provider_organization) { create(:provider_organization) }

    it 'should be cd if link present' do
      create(:cd_org_link, user:, provider_organization:)
      expect(user.cd?(provider_organization)).to be true
    end

    it 'should not be cd if link disabled' do
      create(:cd_org_link, user:, provider_organization:, disabled_at: 1.day.ago)
      expect(user.cd?(provider_organization)).to be false
    end

    it 'should not be cd if link not present' do
      create(:cd_org_link, user: other_user, provider_organization:)
      expect(user.cd?(provider_organization)).to be false
    end
  end
  describe :can_access do
    let(:user) { create(:user) }
    let(:other_user) { create(:user) }
    let(:provider_organization) { create(:provider_organization) }

    it 'should have access if ao link present' do
      create(:ao_org_link, user:, provider_organization:)
      expect(user.can_access?(provider_organization)).to be true
    end

    it 'should not have access if ao link not present' do
      create(:ao_org_link, user: other_user, provider_organization:)
      expect(user.can_access?(provider_organization)).to be false
    end
    it 'should have access if cd link present' do
      create(:cd_org_link, user:, provider_organization:)
      expect(user.can_access?(provider_organization)).to be true
    end

    it 'should not have access if cd link disabled' do
      create(:cd_org_link, user:, provider_organization:, disabled_at: 1.day.ago)
      expect(user.can_access?(provider_organization)).to be false
    end

    it 'should not have access if cd link not present' do
      create(:cd_org_link, user: other_user, provider_organization:)
      expect(user.can_access?(provider_organization)).to be false
    end
  end

  describe :provider_organizations do
    let(:provider_organization) { create(:provider_organization) }
    let(:user) { create(:user) }
    it 'should return orgs with ao_org_link' do
      create(:ao_org_link, provider_organization:, user:)
      expect(user.provider_organizations).to include(provider_organization)
    end
    it 'should return orgs with cd_org_link' do
      create(:cd_org_link, provider_organization:, user:)
      expect(user.provider_organizations).to include(provider_organization)
    end
    it 'should not return orgs with disabaled cd_org_link' do
      create(:cd_org_link, provider_organization:, user:, disabled_at: 2.days.ago)
      expect(user.provider_organizations).to_not include(provider_organization)
    end
    it 'should not return orgs without link' do
      expect(user.provider_organizations).to_not include(provider_organization)
    end
  end
end

describe :validations do
  let(:user) { create(:user) }

  it 'fails on invalid verification_reason' do
    expect do
      user.verification_reason = :fake_reason
    end.to raise_error(ArgumentError)
  end

  it 'allows good verification_reason' do
    expect do
      user.verification_reason = :ao_med_sanction_waived
      user.save
    end.not_to raise_error
  end

  it 'allows blank verification_reason' do
    expect do
      user.verification_reason = ''
      user.save
    end.not_to raise_error
  end

  it 'allows nil verification_reason' do
    expect do
      user.verification_reason = nil
      user.save
    end.not_to raise_error
  end

  it 'fails on invalid verification_status' do
    expect do
      user.verification_status = :fake_status
    end.to raise_error(ArgumentError)
  end

  it 'allows good verification_status' do
    expect do
      user.verification_status = :approved
      user.save
    end.not_to raise_error
  end

  it 'allows nil verification_status' do
    expect do
      user.verification_status = nil
      user.save
    end.not_to raise_error
  end
end

class MockAuthInfo
  attr_reader :provider, :uid, :info

  def initialize(provider, uid, email)
    @provider = provider
    @uid = uid
    @info = Info.new(email)
  end

  class Info
    attr_reader :email

    def initialize(email)
      @email = email
    end
  end
end
