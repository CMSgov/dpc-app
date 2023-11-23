# frozen_string_literal: true

Rails.application.routes.draw do
  devise_for :users
  scope 'portal' do
    # Defines the root path route ("/")
    root 'main#welcome'

    if Rails.env.development?
      require 'sidekiq/web'
      mount Sidekiq::Web, at: '/sidekiq'
      mount Lookbook::Engine, at: "/lookbook"
    end
  end
end
