# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Invitation, type: :model do
  let(:organization) { build(:provider_organization) }

  describe :ao do
    let(:valid_new_ao_invite) { build(:invitation, :ao) }
    describe :create do
      it 'passes validations' do
        expect(valid_new_ao_invite.valid?).to eq(true), valid_new_ao_invite.errors.inspect
      end

      it 'fails if no provider organization' do
        valid_new_ao_invite.provider_organization = nil
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'does not fail if no invited by' do
        valid_new_ao_invite.invited_by = nil
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'fails on blank invitation type' do
        valid_new_ao_invite.invitation_type = nil
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          valid_new_ao_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end

      it 'does not fail on blank given name' do
        valid_new_ao_invite.invited_given_name = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'does not fail on blank family name' do
        valid_new_ao_invite.invited_family_name = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'does not fail on blank phone' do
        valid_new_ao_invite.phone_raw = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'fails on bad email' do
        valid_new_ao_invite.invited_email_confirmation = valid_new_ao_invite.invited_email = 'rob-at-example.com'
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invited_email]).to eq ['is invalid']
      end

      it 'fails on blank email' do
        valid_new_ao_invite.invited_email_confirmation = valid_new_ao_invite.invited_email = ''
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 3
        expect(valid_new_ao_invite.errors[:invited_email]).to eq ["can't be blank", 'is invalid']
        expect(valid_new_ao_invite.errors[:invited_email_confirmation]).to eq ["can't be blank"]
      end

      it 'fails on non-matching email confirmation' do
        valid_new_ao_invite.invited_email_confirmation = 'robert@example.com'
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invited_email_confirmation]).to eq ["doesn't match Invited email"]
      end
    end

    describe :update do
      let(:saved_ao_invite) do
        valid_new_ao_invite.save!
        valid_new_ao_invite
      end
      it 'should allow blank invitation values' do
        saved_ao_invite.invited_given_name = ''
        saved_ao_invite.invited_family_name = ''
        saved_ao_invite.invited_phone = ''
        saved_ao_invite.invited_email = ''
        saved_ao_invite.invited_email_confirmation = ''
        expect(saved_ao_invite.valid?).to eq true
      end

      it 'should allow blank phone_raw' do
        saved_ao_invite.phone_raw = ''
        expect(saved_ao_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        saved_ao_invite.provider_organization = nil
        expect(saved_ao_invite.valid?).to eq false
        expect(saved_ao_invite.errors.size).to eq 1
        expect(saved_ao_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails on blank invitation type' do
        saved_ao_invite.invitation_type = nil
        expect(saved_ao_invite.valid?).to eq false
        expect(saved_ao_invite.errors.size).to eq 1
        expect(saved_ao_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          saved_ao_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end
    end

    describe :expired do
      it 'should not be expired if less than 2 days old' do
        invitation = create(:invitation, :ao, created_at: 47.hours.ago)
        expect(invitation.expired?).to eq false
      end
      it 'should be expired if more than 2 days old' do
        invitation = create(:invitation, :ao, created_at: 49.hours.ago)
        expect(invitation.expired?).to eq true
      end
      it 'should be expired if 2 days old' do
        invitation = create(:invitation, :ao, created_at: 2.days.ago)
        expect(invitation.expired?).to eq true
      end
    end

    describe :accepted do
      it 'should be accepted if has ao_org_link' do
        link = create(:ao_org_link)
        expect(link.invitation.accepted?).to eq true
      end
      it 'should not be accepted if not have ao_org_link' do
        invitation = create(:invitation, :ao)
        expect(invitation.accepted?).to eq false
      end
    end

    describe :match_user do
      let(:ao_invite) { build(:invitation, :ao) }
      let(:user) do
        build(:user, given_name: 'Hugo',
                     family_name: 'Boss',
                     email: ao_invite.invited_email)
      end
      it 'should match user if email correct' do
        expect(ao_invite.match_user?(user)).to eq true
      end
      it 'should match user if email different case' do
        user.email = user.email.upcase_first
        expect(ao_invite.match_user?(user)).to eq true
      end
      it 'should not match user if email not correct' do
        user.email = "not #{ao_invite.invited_email}"
        expect(ao_invite.match_user?(user)).to eq false
      end
    end
  end
  describe :cd do
    let(:valid_new_cd_invite) { build(:invitation, :cd) }
    describe :create do
      it 'passes validations' do
        expect(valid_new_cd_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        valid_new_cd_invite.provider_organization = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails if no invited by' do
        valid_new_cd_invite.invited_by = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_by]).to eq ["can't be blank"]
      end

      it 'fails on blank invitation type' do
        valid_new_cd_invite.invitation_type = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          valid_new_cd_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end

      it 'fails on blank given name' do
        valid_new_cd_invite.invited_given_name = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_given_name]).to eq ["can't be blank"]
      end

      it 'fails on blank family name' do
        valid_new_cd_invite.invited_family_name = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_family_name]).to eq ["can't be blank"]
      end

      it 'fails on fewer than ten digits in phone' do
        valid_new_cd_invite.phone_raw = '877-288-313'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on more than ten digits in phone' do
        valid_new_cd_invite.phone_raw = '877-288-31333'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on blank phone' do
        valid_new_cd_invite.phone_raw = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 2
        expect(valid_new_cd_invite.errors[:phone_raw]).to eq ["can't be blank"]
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on bad email' do
        valid_new_cd_invite.invited_email_confirmation = valid_new_cd_invite.invited_email = 'rob-at-example.com'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_email]).to eq ['is invalid']
      end

      it 'fails on blank email' do
        valid_new_cd_invite.invited_email_confirmation = valid_new_cd_invite.invited_email = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 3
        expect(valid_new_cd_invite.errors[:invited_email]).to eq ["can't be blank", 'is invalid']
        expect(valid_new_cd_invite.errors[:invited_email_confirmation]).to eq ["can't be blank"]
      end

      it 'fails on non-matching email confirmation' do
        valid_new_cd_invite.invited_email_confirmation = 'robert@example.com'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_email_confirmation]).to eq ["doesn't match Invited email"]
      end
    end

    describe :update do
      let(:saved_cd_invite) do
        valid_new_cd_invite.save!
        valid_new_cd_invite
      end
      it 'should allow blank invitation values' do
        saved_cd_invite.invited_given_name = ''
        saved_cd_invite.invited_family_name = ''
        saved_cd_invite.invited_phone = ''
        saved_cd_invite.invited_email = ''
        saved_cd_invite.invited_email_confirmation = ''
        expect(saved_cd_invite.valid?).to eq true
      end

      it 'should allow blank phone_raw' do
        saved_cd_invite.phone_raw = ''
        expect(saved_cd_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        saved_cd_invite.provider_organization = nil
        expect(saved_cd_invite.valid?).to eq false
        expect(saved_cd_invite.errors.size).to eq 1
        expect(saved_cd_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails on blank invitation type' do
        saved_cd_invite.invitation_type = nil
        expect(saved_cd_invite.valid?).to eq false
        expect(saved_cd_invite.errors.size).to eq 1
        expect(saved_cd_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          saved_cd_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end
    end

    describe :expired do
      it 'should not be expired if less than 2 days old' do
        invitation = create(:invitation, :cd, created_at: 47.hours.ago)
        expect(invitation.expired?).to eq false
      end
      it 'should be expired if more than 2 days old' do
        invitation = create(:invitation, :cd, created_at: 49.hours.ago)
        expect(invitation.expired?).to eq true
      end
      it 'should be expired if 2 days old' do
        invitation = create(:invitation, :cd, created_at: 2.days.ago)
        expect(invitation.expired?).to eq true
      end
    end

    describe :accepted do
      it 'should be accepted if has cd_org_link' do
        link = create(:cd_org_link)
        expect(link.invitation.accepted?).to eq true
      end
      it 'should not be accepted if not have cd_org_link' do
        invitation = create(:invitation, :cd)
        expect(invitation.accepted?).to eq false
      end
    end

    describe :match_user do
      let(:cd_invite) { build(:invitation, :cd) }
      let(:user) do
        build(:user, given_name: cd_invite.invited_given_name,
                     family_name: cd_invite.invited_family_name,
                     email: cd_invite.invited_email)
      end
      it 'should match user if names and email correct' do
        expect(cd_invite.match_user?(user)).to eq true
      end
      it 'should match user if names and email different case' do
        user.given_name.upcase!
        user.family_name.downcase!
        user.email = user.email.upcase_first
        expect(cd_invite.match_user?(user)).to eq true
      end
      it 'should not match user if given name not correct' do
        user.given_name = "not #{cd_invite.invited_given_name}"
        expect(cd_invite.match_user?(user)).to eq false
      end
      it 'should not match user if family name not correct' do
        user.family_name = "not #{cd_invite.invited_family_name}"
        expect(cd_invite.match_user?(user)).to eq false
      end
      it 'should not match user if email not correct' do
        user.email = "not #{cd_invite.invited_email}"
        expect(cd_invite.match_user?(user)).to eq false
      end
    end
  end
end
