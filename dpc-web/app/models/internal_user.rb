# frozen_string_literal: true

class InternalUser < ApplicationRecord
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable, :registerable
  devise :database_authenticatable,
         :trackable, :timeoutable,
         :omniauthable, omniauth_providers: [omniauth_provider]

  validates :uid, uniqueness: { scope: :provider }

  def self.from_omniauth(auth)
    where(provider: auth.provider, uid: auth.uid).first_or_create do |user|
      user.email = auth.info.email
      user.password = Devise.friendly_token[0, 20]
      user.name = auth.info.name
      user.github_nickname = auth.info.nickname if ENV.fetch('INTERNAL_AUTH_PROVIDER') == 'github'
    end
  end

  def omniauth_provider
    if ENV.fetch('INTERNAL_AUTH_PROVIDER') == 'okta'
      :oktaoauth
    else
      :github
    end
  end
end
