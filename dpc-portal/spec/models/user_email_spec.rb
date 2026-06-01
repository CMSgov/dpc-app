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

  it 'ensures that creating a new primary email unsets others' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)

    email_old_primary = create(:user_email, csp_user:, primary: true, email: 'old@email.com')
    email_new_primary = create(:user_email, csp_user:, primary: true, email: 'new@email.com')

    expect(email_old_primary.reload.primary).to eq false
    expect(email_new_primary.reload.primary).to eq true
  end

  it 'ensures that setting an email to primary unsets others' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)

    email_old_primary = create(:user_email, csp_user:, primary: true, email: 'old@email.com')
    email_new_primary = create(:user_email, csp_user:, primary: false, email: 'new@email.com')
    expect(email_old_primary.primary).to eq true
    expect(email_new_primary.primary).to eq false

    # Should set email1 to not primary
    email_new_primary.update!(primary: true)
    email_old_primary.reload
    email_new_primary.reload

    expect(email_old_primary.primary).to eq false
    expect(email_new_primary.primary).to eq true
  end
end
