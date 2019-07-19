# frozen_string_literal: true

class User < ApplicationRecord
  has_one :registration, inverse_of: :user

  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable,
  # :trackable, :registerable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :rememberable,
         :validatable, :trackable

  validates :last_name, :first_name, presence: true

  # enum status: %i[unconfirmed pending synthesized production]

  def name
    "#{first_name} #{last_name}"
  end
end
