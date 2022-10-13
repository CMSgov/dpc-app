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
      user = create(:user, id: 50000, first_name: 'Clarissa', last_name: 'Dalloway', email: 'cd@example.com',
                    requested_organization: 'Amalgamated Lint', requested_organization_type: 'primary_care_clinic',
                    address_1: '1234 Shut the Door Ave.', address_2: 'Ste 1000', city: 'Pecoima',
                    state: 'AZ', zip: '12345', agree_to_terms: true, requested_num_providers: 5,
                    created_at: '2019-10-15 18:29:35 UTC', updated_at: '2019-10-15 18:29:35 UTC')
      tag = create(:tag, name: "Wolf")
      user.tags << tag
  
      fixture_csv_content = File.read('spec/fixtures/users.csv')
      FakeFS.with_fresh do
        expect(User.to_csv([50000])).to eq(fixture_csv_content)
      end
    end

    it 'generates CSV of only selected users' do
      user1 = create(:user, id: 50000, first_name: 'Clarissa', last_name: 'Dalloway', email: 'cd@example.com',
        requested_organization: 'Amalgamated Lint', requested_organization_type: 'primary_care_clinic',
        address_1: '1234 Shut the Door Ave.', address_2: 'Ste 1000', city: 'Pecoima',
        state: 'AZ', zip: '12345', agree_to_terms: true, requested_num_providers: 5,
        created_at: '2019-10-15 18:29:35 UTC', updated_at: '2019-10-15 18:29:35 UTC')
      user2 = create(:user, id: 50001, first_name: 'Spongebob', last_name: 'Squarepants', email: 'spbob@example.com',
        requested_organization: 'Amalgamated Lint', requested_organization_type: 'primary_care_clinic',
        address_1: '1234 Bikini Bottom.', address_2: 'Ste 1000', city: 'Pecoima',
        state: 'AZ', zip: '12345', agree_to_terms: true, requested_num_providers: 5,
        created_at: '2019-10-16 18:29:35 UTC', updated_at: '2019-10-16 18:29:35 UTC')
      tag = create(:tag, name: "Wolf")
      user1.tags << tag

      fixture_csv_content = File.read('spec/fixtures/users.csv')
      FakeFS.with_fresh do
        expect(User.to_csv([50000])).to eq(fixture_csv_content)
      end
    end
  
    it 'escapes the html for the correct attributes' do
      user = create(:user, id: 50000, first_name: '?name=<script>Clarissa</script>', last_name: 'Dalloway', email: 'cd@example.com',
                    requested_organization: 'Amalgamated Lint', requested_organization_type: 'primary_care_clinic',
                    address_1: '1234 Shut the Door Ave.', address_2: 'Ste 1000', city: 'Pecoima',
                    state: 'AZ', zip: '12345', agree_to_terms: true, requested_num_providers: 5,
                    created_at: '2019-10-15 18:29:35 UTC', updated_at: '2019-10-15 18:29:35 UTC')
      tag = create(:tag, name: "Wolf")
      user.tags << tag
  
      fixture_csv_content = File.read('spec/fixtures/users_html_escaped.csv')
      FakeFS.with_fresh do
        expect(User.to_csv([50000])).to eq(fixture_csv_content)
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

  describe '#email' do
    it 'must use valid domain' do
      subject.email = 'fake_user@baddomaincom'
      expect(subject).to_not be_valid
    end
  end

  describe 'scopes' do
    describe '.assigned' do
      it 'includes only users with an organization' do
        assigned_user = create(:user, :assigned)
        _unassigned_user = create(:user)

        expect(User.assigned).to match_array([assigned_user])
      end
    end

    describe '.unassigned' do
      it 'includes only users without an organization' do
        _user = create(:user, :assigned)
        unassigned_user = create(:user)

        expect(User.unassigned).to match_array([unassigned_user])
      end
    end

    describe '.assigned_provider' do
      it 'includes users with provider org assignment' do
        user = create(:user, :assigned)
        _vendor_user = create(:user, :vendor)

        expect(User.assigned_provider).to match_array([user])
      end
    end

    describe '.assigned_vendor' do
      it 'includes users with vendor org assignment' do
        _user = create(:user, :assigned)
        vendor_user = create(:user, :vendor)

        expect(User.assigned_vendor).to match_array([vendor_user])
      end
    end

    describe '.vendor' do
      it 'includes users with vendor assigned or requested org' do
        assigned_user = create(:user, :vendor)
        requested_user = create(:user, requested_organization_type: 'health_it_vendor')
        _assigned_prov_user = create(:user, :assigned)
        _requested_prov_user = create(:user)

        expect(User.vendor).to match_array([assigned_user, requested_user])
      end
    end

    describe '.provider' do
      it 'includes users with provider assigned or requested org' do
        assigned_user = create(:user, :assigned)
        requested_user = create(:user)
        _assigned_vendor_user = create(:user, :vendor)
        _requested_vendor_user = create(:user, requested_organization_type: 'health_it_vendor')

        expect(User.provider).to match_array([assigned_user, requested_user])
      end
    end

    describe '.by_keyword' do
      it 'returns users fuzzy-matching keyword by first_name' do
        user = create(:user, first_name: 'Clover')
        _user = create(:user, first_name: 'Summer')

        expect(User.by_keyword('over')).to match_array([user])
      end

      it 'returns users fuzzy-matching keyword by last_name' do
        user = create(:user, last_name: 'Smithfield')
        _user = create(:user, last_name: 'Lee')

        expect(User.by_keyword('Smith')).to match_array([user])
      end

      it 'returns users fuzzy-matching keyword by email' do
        user = create(:user, email: 'clover@doctors.com')
        _user = create(:user, last_name: 'summer@example.com')

        expect(User.by_keyword('doctors')).to match_array([user])
      end
    end
  end
end
