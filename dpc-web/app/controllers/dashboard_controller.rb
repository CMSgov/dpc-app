# frozen_string_literal: true

class DashboardController < ApplicationController
  before_action :authenticate_user!

  def download_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  def show
    @user = current_user
  end
end
