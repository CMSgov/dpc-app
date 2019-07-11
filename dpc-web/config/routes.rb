Rails.application.routes.draw do
  root to: 'public#home'

  match '/home', to: 'public#home', via: :get

  match '/docs', to: 'api_documents#home', via: :get
  match '/docs/user-guide', to: 'api_documents#user_guide', via: :get
end
