# frozen_string_literal: true

module ApplicationHelper
  def title(page_title)
    content_for(:title) { page_title }
  end

  def banner_title(banner_title)
    content_for(:banner_title) { banner_title }
  end

  def current_class?(test_path)
    return 'ds-c-tabs__item--active' if request.path == test_path

    ''
  end

  def meta_tag(tag, text)
    content_for :"meta_#{tag}", text
  end

  def yield_meta_tag(tag, default_text = '')
    content_for?(:"meta_#{tag}") ? content_for(:"meta_#{tag}") : default_text
  end

  def tabs_set(tabs_set)
    content_for(:tabs_set) { tabs_set }
  end

  def formatted_datestr(str)
    return 'No date' if str.blank?

    datetime = DateTime.strptime(str, '%Y-%m-%dT%H:%M:%S')
    datetime.strftime('%m/%d/%Y at %l:%M%p UTC')
  end
end
