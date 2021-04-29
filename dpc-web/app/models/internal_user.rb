# frozen_string_literal: true

class InternalUser < ApplicationRecord
  OKTA_AUTH_ENABLED = ENV.fetch('INTERNAL_AUTH_PROVIDER', '') == 'oktaoauth'
  GITHUB_AUTH_ENABLED = ENV.fetch('INTERNAL_AUTH_PROVIDER', '') == 'github'

  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable, :registerable
  devise :database_authenticatable,
         :trackable, :timeoutable,
         :omniauthable, omniauth_providers: %i[oktaoauth github]

  validates :uid, uniqueness: { scope: :provider }

  def self.from_omniauth(auth)
    where(provider: auth.provider, uid: auth.uid).first_or_create do |user|
      user.email = auth.info.email
      user.password = Devise.friendly_token[0, 20]
      user.name = auth.info.name

      # TODO: Change this to username and store preferred_username from Okta
      user.github_nickname = auth.info.nickname if GITHUB_AUTH_ENABLED
    end
  end
end
