# frozen_string_literal: true

require 'rails_helper'
require 'fakefs/spec_helpers'

RSpec.describe User, type: :model do
  subject { create :user }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

  describe '.to_csv' do
    it 'generates CSV of all users' do
      create(:user, id: 50000, first_name: 'Clarissa', last_name: 'Dalloway', email: 'cd@example.com',
                    requested_organization: 'Amalgamated Lint', requested_organization_type: 'primary_care_clinic',
                    address_1: '1234 Shut the Door Ave.', address_2: 'Ste 1000', city: 'Pecoima',
                    state: 'AZ', zip: '12345', agree_to_terms: true, requested_num_providers: 5,
                    created_at: '2019-10-15 18:29:35 UTC', updated_at: '2019-10-15 18:29:35 UTC')

      fixture_csv_content = File.read('spec/fixtures/users.csv')
      FakeFS.with_fresh do
        expect(User.to_csv).to eq(fixture_csv_content)
      end
    end
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

  describe '#name' do
    it 'returns the full name' do
      expect(subject.name).to eq("#{subject.first_name} #{subject.last_name}")
    end
  end

  describe '#requested_organization' do
    it 'is required' do
      subject.requested_organization = nil
      expect(subject).to_not be_valid
    end
  end

  describe '#requested_organization_type' do
    it 'is required' do
      subject.requested_organization_type = nil
      expect(subject).to_not be_valid
    end

    it 'must be valid' do
      expect { subject.requested_organization_type = 'blah-blah' }.to raise_error(ArgumentError)
    end
  end

  describe '#requested_num_providers' do
    it 'defaults to 0' do
      expect(subject.requested_num_providers).to be_zero
    end

    it 'must be greater than or equal to 0' do
      subject.requested_num_providers = -1
      expect(subject).to_not be_valid
    end

    it 'must be an integer' do
      subject.requested_num_providers = 1.23
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

  describe 'scopes' do
    describe '.assigned' do
      it 'includes only users with an organization' do
        org = create(:organization)
        assigned_user = create(:user, organization: org)
        _unassigned_user = create(:user, organization: nil)

        expect(User.assigned).to match_array([assigned_user])
      end
    end

    describe '.unassigned' do
      it 'includes only users without an organization' do
        org = create(:organization)
        _assigned_user = create(:user, organization: org)
        unassigned_user = create(:user, organization: nil)

        expect(User.unassigned).to match_array([unassigned_user])
      end
    end

    describe '.non_vendor' do
      it 'includes users with non-vendor org or requested org' do
        org = create(:organization, organization_type: 'primary_care_clinic')
        vendor = create(:organization, organization_type: 'health_it_vendor')

        user = create(:user, organization: org)
        unassigned_user = create(:user, organization: nil, requested_organization_type: 'urgent_care')
        _vendor_user = create(:user, organization: vendor)
        _unassigned_vendor_user = create(:user, organization: nil, requested_organization_type: 'health_it_vendor')

        expect(User.non_vendor).to match_array([user, unassigned_user])
      end
    end

    describe '.vendor' do
      it 'includes users with vendor org or requested org' do
        org = create(:organization, organization_type: 'primary_care_clinic')
        vendor = create(:organization, organization_type: 'health_it_vendor')

        _user = create(:user, organization: org)
        _unassigned_user = create(:user, organization: nil, requested_organization_type: 'urgent_care')
        vendor_user = create(:user, organization: vendor)
        unassigned_vendor_user = create(:user, organization: nil, requested_organization_type: 'health_it_vendor')

        expect(User.vendor).to match_array([vendor_user, unassigned_vendor_user])
      end
    end
  end
end
