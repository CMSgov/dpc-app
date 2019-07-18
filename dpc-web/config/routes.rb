Rails.application.routes.draw do
  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'docs#reference', via: :get
  match '/docs/guide', to: 'docs#guide', via: :get
end
