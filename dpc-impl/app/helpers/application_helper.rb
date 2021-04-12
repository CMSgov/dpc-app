# frozen_string_literal: true

module ApplicationHelper
  def yield_meta_tag(tag, default_text = '')
    content_for?(:"meta_#{tag}") ? content_for(:"meta_#{tag}") : default_text
  end
end
