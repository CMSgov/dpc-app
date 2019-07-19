# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Registration, type: :model do
  subject { build :registration, user: user }
  let(:user) { create :user }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

  describe '#user' do
    it 'user is required' do
      subject.user = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#organization' do
    it 'is required' do
      subject.organization = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#address' do
    it 'first line is required' do
      subject.address_1 = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#city' do
    it 'is required' do
      subject.city = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#state' do
    it 'is required' do
      subject.city = nil
      expect(subject).to_not be_valid
    end

    it 'must be valid' do
      subject.state = 'FU'
      expect(subject).to_not be_valid
    end
  end

  describe '#zip' do
    it 'is required' do
      subject.zip = nil
      expect(subject).to_not be_valid
    end

    it 'must be a valid state' do
      subject.zip = '123456-756'
      expect(subject).to_not be_valid
    end
  end
end
