Rails.application.routes.draw do
  devise_for :internal_users, path: 'internal', controllers: {
    sessions: "internal/auth/sessions",
    omniauth_callbacks: "internal/auth/omniauth_callbacks"
  }
  devise_for :users, path: 'users', controllers: {
    confirmations: "confirmations",
    sessions: "users/sessions",
    registrations: "users/registrations",
    passwords: "users/passwords"
  }

  namespace 'internal' do
    resources :users, only: [:index, :show, :edit, :update, :destroy] do
      collection { get :download }
    end
    resources :taggings, only: [:create, :destroy]
    resources :tags, only: [:index, :create, :destroy]
    resources :organizations do

      resources :registered_organizations, only: [:new, :create, :edit, :update, :destroy] do
        match :enable_or_disable, via: [:get, :post, :update]
      end

      match :add_or_delete, via: [:get, :post, :delete]
    end
  end

  authenticated :user do
    root 'portal#show', as: :authenticated_root, via: :get
  end

  authenticated :internal_user do
    root 'internal/users#index', as: :authenticated_internal_root
  end

  match '/portal', to: 'portal#show', via: :get

  resources :organizations, only: [:edit, :update] do
    resources :client_tokens, only: [:new, :create, :destroy]
    resources :public_keys, only: [:new, :create]
  end

  devise_scope :user do
    root to: "devise/sessions#new"
  end

  match '/home', to: redirect("#{ENV['STATIC_SITE_URL']}"), via: :get

  match '/docs', to: redirect("#{ENV['STATIC_SITE_URL']}/docs"), via: :get

  # downloadable files
  match '/download_snippet', to: 'public_keys#download_snippet', as: 'download_snippet', via: :post
  match '/download_jwt_tool', to: 'application#download_jwt_tool', as: 'download_jwt_tool', via: :post
  match '/download_prac_json', to: 'application#download_prac_json', as: 'download_prac_json', via: :post
  match '/download_pt_json', to: 'application#download_pt_json', as: 'download_pt_json', via: :post
  match '/download_postman_collection', to: 'application#download_postman', as: 'download_postman', via: :post

  match '/faq', to: redirect("#{ENV['STATIC_SITE_URL']}/faq"), via: :get
  match '/terms-of-service', to: redirect("#{ENV['STATIC_SITE_URL']}/terms-of-service"), via: :get

  if Rails.env.development?
    require 'sidekiq/web'
    mount Sidekiq::Web, at: '/sidekiq'
    mount LetterOpenerWeb::Engine, at: "/letter_opener"
  end
end
