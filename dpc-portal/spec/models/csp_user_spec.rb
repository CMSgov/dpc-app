# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspUser, type: :model do
  it 'has a user and csp' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user:, csp:)
    expect(csp_user.user).to eq user
    expect(csp_user.csp).to eq csp
  end
end
