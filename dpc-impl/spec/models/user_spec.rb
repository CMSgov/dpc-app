# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  include ApiClientSupport

  describe '#last_name' do
    it 'requires a last name' do
      subject.last_name = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#first_name' do
    it 'requires a first name' do
      subject.first_name = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#name' do
    it 'returns the full name' do
      expect(subject.name).to eq("#{subject.first_name} #{subject.last_name}")
    end
  end

  describe '#implementer' do
    it 'requires an implementer' do
      subject.implementer = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#email' do
    it 'must use valid domain' do
      subject.email = 'fake_user@baddommaincom'
      expect(subject).to_not be_valid
    end
  end

  describe '#agree_to_terms' do
    it 'is required' do
      subject.agree_to_terms = nil
      expect(subject).to_not be_valid
    end

    it 'must be true to create user' do
      subject.agree_to_terms = false
      expect(subject).to_not be_valid
    end
  end

  describe 'before_creates' do
    before { @user = User.invite! }
    it 'run check_impl callback' do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)
      expect(@user).to receive(:check_impl)
      @user.run_callbacks(:create)
    end
  end
end
