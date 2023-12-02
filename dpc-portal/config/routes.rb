# frozen_string_literal: true

Rails.application.routes.draw do
  devise_for :users

  # Defines the root path route ("/")
  root 'main#welcome'

  if Rails.env.development?
    require 'sidekiq/web'
    mount Sidekiq::Web, at: '/sidekiq'
  end

  if Rails.env.development? || ENV["ENV"] == "dev"
    mount Lookbook::Engine, at: "lookbook"
  end
end
