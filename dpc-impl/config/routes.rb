# frozen_string_literal: true

Rails.application.routes.draw do
  devise_for :users, path: 'users', controllers: {
    confirmations: 'confirmations',
    invitations: 'users/invitations',
    sessions: 'users/sessions',
    registrations: 'users/registrations',
    passwords: 'users/passwords'
  }

  authenticated :user do
    root to: 'portal#show', as: :authenticated_root, via: :get
  end

  resources :users do
    match :email_divert, via: [:post]
  end

  match '/portal', to: 'portal#show', via: :get

  devise_scope :user do
    root to: "devise/sessions#new"
  end

  if Rails.env.development?
    require 'sidekiq/web'
    mount Sidekiq::Web, at: '/sidekiq'
  end
end
