# frozen_string_literal: true

Rails.application.routes.draw do
  scope 'impl' do
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

    resource :provider_orgs, path: '/provider_org', only: [:new, :show] do
      match :add, via: [:post]
    end

    resources :client_tokens, only: [:new, :create, :destroy]
    resources :public_keys, only: [:new, :create, :destroy]
    resources :systems, only: [:create]

    match '/members', to: 'portal#index', via: :get
    match '/portal', to: 'portal#show', via: :get
    match '/public-key-faq', to: 'public_keys#index', via: :get

    devise_scope :user do
      root to: "devise/sessions#new"
    end

    if Rails.env.development?
      require 'sidekiq/web'
      mount Sidekiq::Web, at: '/sidekiq'
      mount LetterOpenerWeb::Engine, at: "/letter_opener"
    end
  end
end
