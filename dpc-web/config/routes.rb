Rails.application.routes.draw do
  devise_for :users
  get '/dpc_registrations' => "dpc_registrations#new", as: :user_root
  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'docs#reference', via: :get
  match '/docs/guide', to: 'docs#guide', via: :get

  match '/faq', to: 'pages#faq', via: :get
  match '/support', to: 'pages#support', via: :get

  resources :dpc_registrations, only: %i[new show]
end
