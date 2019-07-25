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

  describe '#opt_in' do
    it 'defaults to false' do
      expect(subject.opt_in).to be_falsy
    end
  end

  describe '#opt_in_status' do
    it 'is required' do
      subject.opt_in_status = nil
      expect(subject).to_not be_valid
    end

    describe 'when first created' do
      it 'defaults to complete when opt_in is false' do
        expect(subject.opt_in_status).to eq('complete')
      end

      it 'defaults to pending when opt_in is true' do
        expect((create :dpc_registration, opt_in: true).opt_in_status).to eq('pending')
      end
    end

    it 'must be valid' do
      expect { subject.opt_in_status = 'blah-blah' }.to raise_error(ArgumentError)
    end

    it 'changes to pending if opt_in changes' do
      subject.update(opt_in: !subject.opt_in)
      expect(subject.opt_in_status).to eq('pending')

      subject.update(opt_in_status: 'complete')
      subject.update(opt_in: !subject.opt_in)
      expect(subject.opt_in_status).to eq('pending')
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
