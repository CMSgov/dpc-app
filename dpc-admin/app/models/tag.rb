# frozen_string_literal: true

class Tag < ApplicationRecord
  has_many :taggings, dependent: :destroy

  validates :name, uniqueness: { case_sensitive: false }, presence: true
end
