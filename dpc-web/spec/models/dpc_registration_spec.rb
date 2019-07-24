# frozen_string_literal: true

RSpec.describe DpcRegistration, type: :model do
  subject { build :dpc_registration }

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

    it 'accepts zip + 4' do
      subject.zip = '12345-6789'
      expect(subject).to be_valid
    end

    it 'must be valid' do
      subject.zip = '123456-756'
      expect(subject).to_not be_valid
    end
  end

  describe '#opt_in' do
    it 'defaults to true' do
      subject.save
      expect(subject.opt_in).to be_truthy
    end
  end

  describe '#status' do
    it 'is required' do
      subject.status = nil
      expect(subject).to_not be_valid
    end

    it 'must be valid' do
      expect { subject.status = 'blah-blah' }.to raise_error(ArgumentError)
    end

    it 'defaults to pending' do
      expect(subject.status).to eq('pending')
    end
  end
end
