# frozen_string_literal: true

module Internal
  module TagsHelper
    def confirm_text(tag)
      "Are you sure? #{tag.taggings.count} records have this tag."
    end

    def available_tags(tag_type)
      Tag.where.not(id: tag_type.tag_ids)
    end
  end
end
