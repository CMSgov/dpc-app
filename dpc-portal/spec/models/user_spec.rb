# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  let(:user_provider) { 'logindotgov' }
  let(:user_uid) { 'user-uid' }
  let(:provider_data) { MockAuthInfo.new(user_provider, user_uid, 'user@example.com') }

  it 'creates when no records' do
    expect(User.count).to eq 0
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
