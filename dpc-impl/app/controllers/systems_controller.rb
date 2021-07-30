# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def create
    binding.pry
  end
end