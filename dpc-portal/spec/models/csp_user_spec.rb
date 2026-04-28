# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspUser, type: :model do
  it 'has a user' do
    user = create(:user)
    csp = create(:csp)
    csp_user = create(:csp_user, user_id: user.id, csp_id: csp.id)
    expect(csp_user.user).to eq user
  end
end
