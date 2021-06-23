# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Admin, type: :model do
  describe '.from_omniauth(auth)' do
    it 'finds existing admin based on uid and provider' do
      _irrelevant_admin = create(:admin, uid: '919191', provider: 'github')
      existing_admin = create(:admin, uid: '828282', provider: 'github')

      auth = OmniAuth::AuthHash.new(
        provider: 'github',
        uid: '828282',
        info: {
          nickname: 'whereisnemo',
          email: 'test@cms.hhs.gov',
          name: 'Nemo',
          image: 'https://avatars3.githubusercontent.com/u/111'
        }
      )

      expect(Admin.from_omniauth(auth)).to eq(existing_admin)
    end

    it 'creates new admin based on auth hash' do
      auth = OmniAuth::AuthHash.new(
        provider: 'github',
        uid: '828282',
        info: {
          nickname: 'whereisnemo',
          email: 'test@cms.hhs.gov',
          name: 'Nemo',
          image: 'https://avatars3.githubusercontent.com/u/111'
        }
      )

      expect { Admin.from_omniauth(auth) }.to change(Admin, :count).by(1)
      expect(
        Admin.last.attributes
          .fetch_values('github_nickname', 'email', 'name', 'uid', 'provider')
      ).to eq(['whereisnemo', 'test@cms.hhs.gov', 'Nemo', '828282', 'github'])
    end
  end
end
