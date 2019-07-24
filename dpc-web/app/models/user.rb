# frozen_string_literal: true

class User < ApplicationRecord
  has_one :dpc_registration, inverse_of: :user

  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable,
  # :trackable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :rememberable,
         :validatable, :trackable, :registerable

  validates :last_name, :first_name, presence: true

  def name
    "#{first_name} #{last_name}"
  end
end
