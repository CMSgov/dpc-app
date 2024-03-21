# frozen_string_literal: true

# Sets up the application's routes. Note that this is served
# under the /portal prefix, as configured in the application.rb
# and config.ru via config.relative_url_root.
#
Rails.application.routes.draw do
  devise_for :users, controllers: { omniauth_callbacks: 'login_dot_gov' }
  devise_scope :user do
    get '/users/auth/failure', to: 'login_dot_gov#failure', as: 'login_dot_gov_failure'
  end

  # Defines the root path route ("/")
  root 'main#welcome'

  # Route /portal to the main controller. This is a necessary workaround since
  # Lookbook mimics requests from the root_path, which will return /portal.
  # However, to complete the mimicing, it uses the Rails.application.routes.recognize_path
  # method, which does not work correctly for applications served on a subpath.
  match '/portal', to: 'main#welcome', via: :get

  resources :organizations, only: [:index, :show, :new, :create] do
    resources :client_tokens, only: [:new, :create, :destroy]
    resources :public_keys, only: [:new, :create, :destroy]
    resources :ip_addresses, only: [:new, :create, :destroy]
    resources :credential_delegate_invitations, only: [:new, :create, :destroy] do
      get 'success', on: :member
    end
    get 'tos_form', on: :member
    post 'sign_tos', on: :member
    get 'success', on: :member
  end

  match '/download_snippet', to: 'public_keys#download_snippet', as: 'download_snippet', via: :post

  if Rails.env.development?
    require 'sidekiq/web'
    mount Sidekiq::Web, at: '/sidekiq'
  end

  if Rails.env.development? || ENV["ENV"] == "dev"
    mount Lookbook::Engine, at: "lookbook"
  end
end
