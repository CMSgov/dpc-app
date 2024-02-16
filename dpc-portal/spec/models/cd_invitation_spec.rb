# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdInvitation, type: :model do
  let(:valid_cd_invite) do
    CdInvitation.new(given_name: 'Bob',
                     family_name: 'Hogan',
                     phone_raw: '877-288-3131',
                     email: 'bob@example.com',
                     email_confirmation: 'bob@example.com')
  end
  it 'passes validations' do
    expect(valid_cd_invite.valid?).to eq true
  end

  it 'fails on blank given name' do
    valid_cd_invite.given_name = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:given_name]).to eq ["can't be blank"]
  end

  it 'fails on blank family name' do
    valid_cd_invite.family_name = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:family_name]).to eq ["can't be blank"]
  end

  it 'fails on fewer-than-nine digits in phone' do
    valid_cd_invite.phone_raw = '877-288-313'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:phone]).to eq ['is invalid']
  end

  it 'fails on more-than-nine digits in phone' do
    valid_cd_invite.phone_raw = '877-288-31333'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:phone]).to eq ['is invalid']
  end

  it 'fails on blank phone' do
    valid_cd_invite.phone_raw = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 2
    expect(valid_cd_invite.errors[:phone_raw]).to eq ["can't be blank"]
    expect(valid_cd_invite.errors[:phone]).to eq ['is invalid']
  end

  it 'fails on bad email' do
    valid_cd_invite.email_confirmation = valid_cd_invite.email = 'rob-at-example.com'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:email]).to eq ['is invalid']
  end

  it 'fails on blank email' do
    valid_cd_invite.email_confirmation = valid_cd_invite.email = ''
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 3
    expect(valid_cd_invite.errors[:email]).to eq ["can't be blank", 'is invalid']
    expect(valid_cd_invite.errors[:email_confirmation]).to eq ["can't be blank"]
  end

  it 'fails on non-matching email confirmation' do
    valid_cd_invite.email_confirmation = 'robert@example.com'
    expect(valid_cd_invite.valid?).to eq false
    expect(valid_cd_invite.errors.size).to eq 1
    expect(valid_cd_invite.errors[:email_confirmation]).to eq ["doesn't match Email"]
  end
end
