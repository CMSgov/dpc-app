Rails.application.routes.draw do
  devise_for :internal_users, path: 'internal', controllers: {
    sessions: "internal/auth/sessions",
    omniauth_callbacks: "internal/auth/omniauth_callbacks"
  }
  devise_for :users, path: 'users', controllers: {
    sessions: "users/sessions",
    registrations: "users/registrations",
    passwords: "users/passwords"
  }

  namespace 'internal' do
    resources :users, only: [:index, :show, :edit, :update] do
      collection { get :download }
    end
    resources :taggings, only: [:create, :destroy]
    resources :tags, only: [:index, :create, :destroy]
    resources :organizations
  end

  authenticated :user do
    root 'dashboard#show', as: :authenticated_root, via: :get
  end

  authenticated :internal_user do
    root 'internal/users#index', as: :authenticated_internal_root
  end

  match '/dashboard', to: 'dashboard#show', via: :get

  resources :organizations, only: [:edit, :update] do
    resources :client_tokens, only: [:new, :create]
    resources :public_keys, only: [:new, :create]
  end

  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'pages#reference', via: :get
  # match '/docs/guide', to: 'pages#guide', via: :get
  match '/faq', to: 'pages#faq', via: :get
  match '/support', to: 'pages#support', via: :get
  match '/terms-of-service', to: 'pages#terms_of_service', via: :get
end
