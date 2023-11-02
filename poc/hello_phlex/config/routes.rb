Rails.application.routes.draw do
  resources :hello, only: [:index]
  # Define your application routes per the DSL in https://guides.rubyonrails.org/routing.html

  # Defines the root path route ("/")
  # root "articles#index"
  root 'hello#index'

  # For the purposes of this PoC, set everything to link to the main page
  get '/home', to: 'hello#index'
  get '/docs', to: 'hello#index'
  get '/faq', to: 'hello#index'
  get '/terms-of-service', to: 'hello#index'

  get '/users/new', to: 'hello#index#new'
  get '/session/new', to: 'hello#index#new'
end
