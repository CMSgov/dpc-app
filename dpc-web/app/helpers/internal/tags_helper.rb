# frozen_string_literal: true

module Internal
  module TagsHelper
    def confirm_text(tag)
      "Are you sure? #{tag.taggings.count} records have this tag."
    end
  end
end
