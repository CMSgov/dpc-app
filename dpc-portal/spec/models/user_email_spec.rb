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

  it 'ensures that creating a new verified email unsets others' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)

    email_old_verified = create(:user_email, csp_user:, verified: true, email: 'old@email.com')
    email_new_verified = create(:user_email, csp_user:, verified: true, email: 'new@email.com')

    expect(email_old_verified.reload.verified).to eq false
    expect(email_new_verified.reload.verified).to eq true
  end

  it 'ensures that setting an email to verified unsets others' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)

    email_old_verified = create(:user_email, csp_user:, verified: true, email: 'old@email.com')
    email_new_verified = create(:user_email, csp_user:, verified: false, email: 'new@email.com')
    expect(email_old_verified.verified).to eq true
    expect(email_new_verified.verified).to eq false

    # Should set email1 to not verified
    email_new_verified.update!(verified: true)
    email_old_verified.reload
    email_new_verified.reload

    expect(email_old_verified.verified).to eq false
    expect(email_new_verified.verified).to eq true
  end
end
