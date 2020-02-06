# frozen_string_literal: true

class InternalUser < ApplicationRecord
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable, :registerable
  devise :database_authenticatable,
         :trackable, :timeoutable,
         :omniauthable, omniauth_providers: %i[oktaoauth]

  validates :uid, uniqueness: { scope: :provider }

  # TODO: Clean up old Github users if needed
  def self.from_omniauth(auth)
    where(provider: auth.provider, uid: auth.uid).first_or_create do |user|
      user.email = auth.info.email
      user.password = Devise.friendly_token[0, 20]
      user.name = auth.info.name
    end
  end
end
