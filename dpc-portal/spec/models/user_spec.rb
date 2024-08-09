# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  describe 'create from factory' do
    it 'should create' do
      expect do
        create(:user)
      end.to change { User.count }.by 1
    end
  end

  describe :ao do
    let(:user) { create(:user) }
    let(:other_user) { create(:user) }
    let(:provider_organization) { create(:provider_organization) }
    let(:other_organization) { create(:provider_organization) }

    it 'should be ao if link present' do
      create(:ao_org_link, user:, provider_organization:)
      expect(user.ao?(provider_organization)).to be true
    end

    it 'should not be ao if link not present' do
      create(:ao_org_link, user: other_user, provider_organization:)
      expect(user.ao?(provider_organization)).to be false
    end

    it 'should not be ao if only cd link present' do
      create(:cd_org_link, user:, provider_organization:)
      expect(user.ao?(provider_organization)).to be false
    end

    it 'should accept multiple organizations' do
      create(:ao_org_link, user:, provider_organization:)
      create(:cd_org_link, user:, provider_organization: other_organization)
      expect(user.ao?([provider_organization, other_organization])).to be true
    end

    it 'should accept empty array' do
      expect(user.ao?([])).to be false
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

  describe :provider_links do
    let(:provider_organization) { create(:provider_organization) }
    let(:user) { create(:user) }
    it 'should return orgs with ao_org_link' do
      link = create(:ao_org_link, provider_organization:, user:)
      expect(user.provider_links).to include(link)
    end
    it 'should return orgs with cd_org_link' do
      link = create(:cd_org_link, provider_organization:, user:)
      expect(user.provider_links).to include(link)
    end
    it 'should not return orgs with disabaled cd_org_link' do
      link = create(:cd_org_link, provider_organization:, user:, disabled_at: 2.days.ago)
      expect(user.provider_links).to_not include(link)
    end
    it 'should not return orgs without link' do
      expect(user.provider_links).to be_empty
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

  describe :audits do
    let(:user) { create(:user) }
    it 'should not audit email' do
      user.update(email: 'new_email@test.com')
      expect(user.audits.count).to eq 0
    end
    it 'should not audit given_name' do
      user.update(given_name: 'Friedrich')
      expect(user.audits.count).to eq 0
    end
    it 'should not audit family_name' do
      user.update(family_name: 'Nietszche')
      expect(user.audits.count).to eq 0
    end
    it 'should not audit pac_id' do
      user.update(pac_id: 'some-new-id')
      expect(user.audits.count).to eq 0
    end
    it 'should audit verification_status' do
      user.update(verification_status: :rejected)
      expect(user.audits.count).to eq 1
    end
    it 'should audit verification_reason' do
      user.update(verification_reason: :ao_med_sanctions)
      expect(user.audits.count).to eq 1
    end
    it 'should audit verification_status and _reason together' do
      user.update(verification_status: :rejected, verification_reason: :ao_med_sanctions)
      expect(user.audits.count).to eq 1
    end
  end
end
