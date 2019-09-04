# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InternalUser, type: :model do
  # TODO validate uniquness of UID per provider

  describe '.from_omniauth(auth)' do
    it 'finds existing internal user based on uid and provider' do
      _irrelevant_internal_user = create(:internal_user, uid: '919191', provider: 'github')
      existing_internal_user = create(:internal_user, uid: '828282', provider: 'github')

      auth = OmniAuth::AuthHash.new({
        provider: 'github',
        uid: '828282',
        info: {
          nickname: 'whereisnemo',
          email: 'test@cms.hhs.gov',
          name: 'Nemo',
          image: 'https://avatars3.githubusercontent.com/u/111'
        }
      })

      expect(InternalUser.from_omniauth(auth)).to eq(existing_internal_user)
    end

    it 'creates new internal user based on auth hash' do
      auth = OmniAuth::AuthHash.new({
        provider: 'github',
        uid: '828282',
        info: {
          nickname: 'whereisnemo',
          email: 'test@cms.hhs.gov',
          name: 'Nemo',
          image: 'https://avatars3.githubusercontent.com/u/111'
        }
      })

      expect{ InternalUser.from_omniauth(auth) }.to change(InternalUser, :count).by(1)
      expect(
        InternalUser.last.attributes.
          fetch_values('github_nickname', 'email', 'name', 'uid', 'provider')
      ).to eq(['whereisnemo', 'test@cms.hhs.gov', 'Nemo', '828282', 'github'])
    end
  end
end
