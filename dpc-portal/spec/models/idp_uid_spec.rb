# frozen_string_literal: true

require 'rails_helper'

RSpec.describe IdpUid, type: :model do
  it 'has a user' do
    user = create(:user)
    idp_uid = create(:idp_uid, user_id: user.id)
    expect(idp_uid.user).to eq user
  end
end
