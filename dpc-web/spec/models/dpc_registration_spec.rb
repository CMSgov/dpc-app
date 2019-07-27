# frozen_string_literal: true

RSpec.describe DpcRegistration, type: :model do
  subject { create :dpc_registration }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

  describe '#user' do
    it 'user is required' do
      subject.user = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#access_level' do
    it 'is required' do
      subject.access_level = nil
      expect(subject).to_not be_valid
    end

    it 'must be valid' do
      expect { subject.access_level = 'blah-blah' }.to raise_error(ArgumentError)
    end

    it 'defaults to no_access' do
      expect(subject.access_level).to eq('no_access')
    end
  end
end
