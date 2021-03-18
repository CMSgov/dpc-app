# frozen_string_literal: true

module ApplicationHelper
  def formatted_datestr(str)
    return 'No date' if str.blank?

    datetime = DateTime.strptime(str, '%Y-%m-%dT%H:%M:%S')
    datetime.strftime('%m/%d/%Y at %l:%M%p UTC')
  end

  def meta_tag(tag, text)
    content_for :"meta_#{tag}", text
  end

  def title(page_title)
    content_for(:title) { page_title }
  end

  def yield_meta_tag(tag, default_text = '')
    content_for?(:"meta_#{tag}") ? content_for(:"meta_#{tag}") : default_text
  end
end
