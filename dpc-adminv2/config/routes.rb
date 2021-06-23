Rails.application.routes.draw do
  scope 'adminv2' do
    devise_for :admin, path: '', controllers: {
      sessions: 'auth/sessions',
      omniauth_callbacks: 'auth/omniauth_callbacks'
    }

    root to: 'implementers#index', via: :get

  end
end
