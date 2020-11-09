# frozen_string_literal: true

module Taggable
  extend ActiveSupport::Concern
  included do
    has_many :taggings, as: :taggable
    has_many :tags, through: :taggings
  end

  class_methods do
    def with_tags(*tags)
      joins(:tags)
        .where(tags: { name: tags })
        .group(:id)
        .having(
          Tag.arel_table[Arel.star]
             .count.gteq(tags.length)
        )
    end
  end
end
