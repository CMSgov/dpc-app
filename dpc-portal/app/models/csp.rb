# frozen_string_literal: true

# Class for defining CSPs
class Csp < ApplicationRecord
  has_many :csp_users
  has_many :users, through: :csp_users
end
