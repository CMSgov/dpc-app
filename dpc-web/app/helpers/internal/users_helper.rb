# frozen_string_literal: true

module Internal
  module UsersHelper
    def available_tags(user)
      Tag.where.not(id: user.tag_ids)
    end
  end
end
