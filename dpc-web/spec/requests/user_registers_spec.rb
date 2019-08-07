# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'User Registers for Access', type: :request do
  let(:user) { attributes_for :user, organization_type: 'speciality_clinic' }

  it 'creates a new user account' do
    expect { post '/users', params: { user: user } }.to change(User, :count)
  end

  it 'new user account contains correct values' do
    post '/users', params: { user: user }
    stored_user = User.first

    user.delete(:password)
    user.delete(:password_confirmation)

    user.each_pair do |key, value|
      expect(value).to eq(stored_user[key])
    end
  end

  it 'creates an associated DPC Registration' do
    post '/users', params: { user: user }
    expect { follow_redirect! }.to change(DpcRegistration, :count)

    dpc_user = DpcRegistration.first.user
    expect(dpc_user.email).to eq(user[:email])
  end
end
