# frozen_string_literal: true

require 'rails_helper'

RSpec.describe UserEmail, type: :model do
  it 'has a csp_user' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)
    user_email = create(:user_email, csp_user:)
    expect(user_email.csp_user).to eq csp_user
  end
end
