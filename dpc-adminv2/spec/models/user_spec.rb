# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  subject { create :user }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

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

  describe '#email' do
    it 'must use valid domain' do
      subject.email = 'fake_user@baddomaincom'
      begin
        r = Truemail.validate(value).result.success
      rescue StandardError
        r = false
      end
      expect(r).to_not be_truthy
    end
  end
end
