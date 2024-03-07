# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Invitation, type: :model do
  let(:organization) { build(:provider_organization) }
  let(:authorized_official) { build(:user) }
  let(:valid_new_cd_invite) { build(:invitation) }

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
      expect(valid_new_cd_invite.errors[:invited_by]).to eq ['must exist']
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
end
