# frozen_string_literal: true

class ApiDocumentsController < ApplicationController
  require 'redcarpet'

  def home
    renderer = Redcarpet::Render::HTML.new()
    @markdown = Redcarpet::Markdown.new(renderer)
  end
end
