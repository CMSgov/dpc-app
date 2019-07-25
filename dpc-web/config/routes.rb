Rails.application.routes.draw do
  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'docs#reference', via: :get
  match '/docs/guide', to: 'docs#guide', via: :get

  match '/faq', to: 'pages#faq', via: :get
  match '/support', to: 'pages#support', via: :get

  match '/implementationguide/', to: "ig#index", via: :get
end
