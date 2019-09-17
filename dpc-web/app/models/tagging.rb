# frozen_string_literal: true

class Tagging < ApplicationRecord
  belongs_to :tag
  belongs_to :taggable, polymorphic: true

  validates :tag, presence: true
  validates :taggable, presence: true

  delegate :name, to: :tag, prefix: true
end
