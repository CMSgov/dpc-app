# frozen_string_literal: true

class User < ApplicationRecord

  validates :email, presence: true, domain_exists: true
  validates :last_name, :first_name, presence: true

  def name
    "#{first_name} #{last_name}"
  end
end
