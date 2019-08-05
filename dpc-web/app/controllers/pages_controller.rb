# frozen_string_literal: true

class PagesController < ApplicationController
  before_action :load_markdown

  protected

  def load_markdown
    # Assume markdown is named the same as the action.
    file_path = lookup_context.find_template("#{controller_path}/#{action_name}").identifier.sub('.html.erb', '.md')

    return unless File.exist?(file_path)

    md_content = File.read(file_path)
    @html_content = Kramdown::Document.new(md_content).to_html
  end
end
