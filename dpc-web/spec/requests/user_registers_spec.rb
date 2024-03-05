# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'User Registers for Access', type: :request do
  let(:user) { attributes_for :user, requested_organization_type: 'speciality_clinic' }

  it 'creates a new user account' do
    expect { post '/users', params: { user: } }.to change(User, :count)
  end

  it 'new user account contains correct values' do
    post '/users', params: { user: }
    stored_user = User.first

    user.delete(:password)
    user.delete(:password_confirmation)
    user.delete(:confirmed_at)

    user.each_pair do |key, value|
      expect(value).to eq(stored_user[key])
    end
  end
end
