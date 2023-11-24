# frozen_string_literal: true

# Handles entry
class MainController < ApplicationController
  def welcome
    render Alert::Component.new(text: "test")
  end
end
