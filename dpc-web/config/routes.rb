Rails.application.routes.draw do
  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'public#docs', via: :get
end
