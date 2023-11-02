# frozen_string_literal: true

class HelloController < ApplicationController
  layout -> { ApplicationLayout }
  
  def index
    render Hello::IndexView.new
  end
end
