# frozen_string_literal: true

# Manages static pages
class StaticPagesController < ApplicationController
  def system_use_agreement
    render layout: 'static_page'
  end
end
