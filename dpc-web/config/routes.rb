Rails.application.routes.draw do
  devise_for :users, path: 'users', controllers: {
    confirmations: "confirmations",
    sessions: "users/sessions",
    registrations: "users/registrations",
    passwords: "users/passwords"
  }

  authenticated :user do
    root 'portal#show', as: :authenticated_root, via: :get
  end

  resources :organizations, only: [:edit, :update] do
    resources :client_tokens, only: [:new, :create, :destroy]
    resources :public_keys, only: [:new, :create, :destroy]
  end

  devise_scope :user do
    root to: "devise/sessions#new"
  end

  match '/home', to: redirect("#{ENV['STATIC_SITE_URL']}"), via: :get

  match '/docs', to: redirect("#{ENV['STATIC_SITE_URL']}/docsV1.html"), via: :get

  # downloadable files
  match '/download_snippet', to: 'public_keys#download_snippet', as: 'download_snippet', via: :post
  match '/download_jwt_tool', to: 'application#download_jwt_tool', as: 'download_jwt_tool', via: :post
  match '/download_prac_json', to: 'application#download_prac_json', as: 'download_prac_json', via: :post
  match '/download_pt_json', to: 'application#download_pt_json', as: 'download_pt_json', via: :post
  match '/download_postman_collection', to: 'application#download_postman', as: 'download_postman', via: :post

  match '/faq', to: redirect("#{ENV['STATIC_SITE_URL']}/faq.html"), via: :get
  match '/terms-of-service', to: redirect("#{ENV['STATIC_SITE_URL']}/terms-of-service.html"), via: :get

  if Rails.env.development?
    mount LetterOpenerWeb::Engine, at: "/letter_opener"
  end
end
