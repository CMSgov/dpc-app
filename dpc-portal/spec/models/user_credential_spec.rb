require 'rails_helper'

RSpec.describe UserCredential, type: :model do
  it 'has a user' do
    user = create(:user)
    user_credential = create(:user_credential, user_id: user.id)
    expect(user_credential.user).to eq user
  end
end
