# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Invitation, type: :model do
  let(:organization) { create(:provider_organization) }
  let(:authorized_official) { create(:user) }
  let(:valid_cd_invite) do
    Invitation.new(provider_organization: organization,
                   invited_by: authorized_official,
                   invitation_type: :credential_delegate,
                   invited_given_name: 'Bob',
                   invited_family_name: 'Hogan',
                   phone_raw: '877-288-3131',
                   invited_email: 'bob@example.com',
                   invited_email_confirmation: 'bob@example.com')
  end
  it 'passes validations' do
    expect(valid_cd_invite.valid?).to eq true
  end

  it 'fails if no provider organization' do
    valid_cd_invite.provider_organization = nil
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:provider_organization]).to eq ['must exist']
  end

  it 'fails if no invited by' do
    valid_cd_invite.invited_by = nil
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_by]).to eq ['must exist']
  end

  it 'fails on blank invitation type' do
    valid_cd_invite.invitation_type = nil
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invitation_type]).to eq ["can't be blank"]
  end

  it 'fails on invalid invitation type' do
    expect do
      valid_cd_invite.invitation_type = :birthday_party
    end.to raise_error(ArgumentError)
  end

  it 'fails on blank given name' do
    valid_cd_invite.invited_given_name = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_given_name]).to eq ["can't be blank"]
  end

  it 'fails on blank family name' do
    valid_cd_invite.invited_family_name = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_family_name]).to eq ["can't be blank"]
  end

  it 'fails on fewer than ten digits in phone' do
    valid_cd_invite.phone_raw = '877-288-313'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_phone]).to eq ['is invalid']
  end

  it 'fails on more than ten digits in phone' do
    valid_cd_invite.phone_raw = '877-288-31333'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_phone]).to eq ['is invalid']
  end

  it 'fails on blank phone' do
    valid_cd_invite.phone_raw = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 2
    expect(valid_cd_invite.errors[:phone_raw]).to eq ["can't be blank"]
    expect(valid_cd_invite.errors[:invited_phone]).to eq ['is invalid']
  end

  it 'fails on bad email' do
    valid_cd_invite.invited_email_confirmation = valid_cd_invite.invited_email = 'rob-at-example.com'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_email]).to eq ['is invalid']
  end

  it 'fails on blank email' do
    valid_cd_invite.invited_email_confirmation = valid_cd_invite.invited_email = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 3
    expect(valid_cd_invite.errors[:invited_email]).to eq ["can't be blank", 'is invalid']
    expect(valid_cd_invite.errors[:invited_email_confirmation]).to eq ["can't be blank"]
  end

  it 'fails on non-matching email confirmation' do
    valid_cd_invite.invited_email_confirmation = 'robert@example.com'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:invited_email_confirmation]).to eq ["doesn't match Invited email"]
  end
end
