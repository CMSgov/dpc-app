Rails.application.routes.draw do
  devise_for :internal_users, path: 'internal', controllers: {
    sessions: "internal/auth/sessions",
    passwords: "internal/auth/passwords",
    omniauth_callbacks: "internal/auth/omniauth_callbacks"
  }
  devise_for :users, path: 'users', controllers: {
    sessions: "users/sessions",
    registrations: "users/registrations",
    passwords: "users/passwords"
  }

  namespace 'internal' do
    resources :users, only: [:index]
  end

  authenticated :user do
    root 'dpc_registrations#show', as: :authenticated_root
  end

  authenticated :internal_user do
    root 'internal/users#index', as: :authenticated_internal_root
  end

  get '/dpc_registrations' => "dpc_registrations#new", as: :user_root


  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'pages#reference', via: :get
  # match '/docs/guide', to: 'pages#guide', via: :get
  match '/faq', to: 'pages#faq', via: :get
  match '/support', to: 'pages#support', via: :get
  match '/terms-of-service', to: 'pages#terms_of_service', via: :get

  match '/profile', to: 'dpc_registrations#profile', via: :get

  resources :dpc_registrations, only: %i[new show]
end
