# frozen_string_literal: true

class ApiDocumentsController < ApplicationController
  def home
    # Assume markdown is named the same as the action.
    file_path = lookup_context.find_template("#{controller_path}/#{action_name}")
                               .identifier.sub(".html.erb", ".md")

    md_content = File.read(file_path)
    @html_content = Kramdown::Document.new(md_content).to_html
  end
end
