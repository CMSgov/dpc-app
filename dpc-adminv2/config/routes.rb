Rails.application.routes.draw do
  scope 'adminv2' do
    devise_for :admin, path: '', controllers: {
      sessions: 'auth/sessions',
      omniauth_callbacks: 'auth/omniauth_callbacks'
    }

    resources :users, path: '/u', only: [:index, :show, :edit, :update, :destroy] do
      collection { get :download }
    end

    resources :implementers, path: '/imp', only: [:index, :show] do
      collection { get :download }
    end

    resources :provider_orgs, path: '/po', only: [:index, :show] do
      collection { get :download }
    end

    root to: 'implementers#index', via: :get

    if Rails.env.development?
      require 'sidekiq/web'
      mount Sidekiq::Web, at: '/sidekiq'
      mount LetterOpenerWeb::Engine, at: "/letter_opener"
    end
  end
end
