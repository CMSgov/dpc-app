# frozen_string_literal: true

# Class for defining CSPs
class Csp < ApplicationRecord
  has_many :csp_users
  has_many :users, through: :csp_users

  scope :active, lambda {
    where(start_date: ..Time.current)
      .where(end_date: [Time.current..., nil])
  }
end
