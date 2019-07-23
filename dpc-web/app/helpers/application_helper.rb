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
end
