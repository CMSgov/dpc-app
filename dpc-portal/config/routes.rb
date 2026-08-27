# frozen_string_literal: true

# Sets up the application's routes.
Rails.application.routes.draw do
  # Former devise routes
  get '/auth/failure', to: 'csp#failure', as: 'csp_failure'
  get '/auth/logged_out', to: 'users/sessions#logged_out'
  get '/auth/no_account', to: 'csp#no_account', as: 'no_account'
  delete '/logout', to: 'csp#logout', as: 'csp_logout'
  get 'active', to: 'users/sessions#active', as: 'active'
  get 'timeout', to: 'users/sessions#timeout', as: 'timeout'
  get '/users/sign_in', to: 'users/sessions#new', as: 'sign_in'
  delete '/users/sign_out', to: 'users/sessions#destroy', as: 'destroy_user_session'
  get '/auth/id_me/callback', to: 'id_me#openid_connect'
  get '/auth/login_dot_gov/callback', to: 'login_dot_gov#openid_connect'
  get '/auth/clear/callback', to: 'clear#openid_connect'

  post '/update', to: 'csp#update'

  # Defines the root path route ("/")
  root 'organizations#index'

  resources :organizations, only: [:index, :show] do
    resources :client_tokens, only: [:new, :create, :destroy]
    resources :public_keys, only: [:new, :create, :destroy]
    resources :ip_addresses, only: [:new, :create, :destroy]
    resources :cd_org_links, only: [:destroy]
    resources :credential_delegate_invitations, only: [:new, :create, :destroy] do
      get 'success', on: :member
    end
    resources :invitations, only: [] do
      member do
        scope ':token', constraints: { token: /[A-Za-z0-9]{24}/ } do
          get '/', action: :show, as: ''
          get 'accept', action: :accept
          post 'confirm', action: :confirm
          post 'register', action: :register
          post 'login', action: :login
          post 'renew', action: :renew
          get 'confirm_cd', action: :confirm_cd
          get 'set_idp_token', action: :set_idp_token
        end
      end
    end
    get 'tos_form', on: :member
    post 'sign_tos', on: :member
  end

  match '/download_snippet', to: 'public_keys#download_snippet', as: 'download_snippet', via: :post
  get 'system-use-agreement', to: 'static_pages#system_use_agreement'

  if Rails.env.development? || ENV["ENV"] == "dev" || ENV["ENV"] == "test"
    mount Lookbook::Engine, at: "lookbook"
  end
end
