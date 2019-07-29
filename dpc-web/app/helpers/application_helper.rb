# frozen_string_literal: true

module ApplicationHelper
  def title(page_title)
    content_for(:title) { page_title }
  end

  def syntax_highlight(text)
    # Initialized in config/initializers/rouge_highlighter.rb
    html = HighlightSource.render(text)
    html.html_safe
  end

  def current_class?(test_path)
    return 'ds-c-tabs__item--active' if request.path == test_path

    ''
  end

  def meta_tag(tag, text)
    content_for :"meta_#{tag}", text
  end

  def yield_meta_tag(tag, default_text='')
    content_for?(:"meta_#{tag}") ? content_for(:"meta_#{tag}") : default_text
  end

end
