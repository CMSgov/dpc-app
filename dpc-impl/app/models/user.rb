# frozen_string_literal: true

class User < ApplicationRecord
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable and :omniauthable
  devise :database_authenticatable, :registerable,
         :recoverable, :validatable,
         :trackable, :timeoutable

  validates :first_name, :last_name, :implementer, presence: true
  validates :email, domain_exists: true
  validate :password_complexity
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  def name
    "#{first_name} #{last_name}"
  end

  private

  def password_complexity
    return if password.nil?

    password_regex = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@\#\$\&*\-])/

    return if password.match? password_regex

    errors.add :password, 'must include at least one number, one lowercase letter,
                           one uppercase letter, and one special character (!@#$&*-)'
  end
end
