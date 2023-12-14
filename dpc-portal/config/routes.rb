# frozen_string_literal: true

# Sets up the application's routes. Note that this is served
# under the /portal prefix, as configured in the application.rb 
# and config.ru via config.relative_url_root.
#
Rails.application.routes.draw do
  devise_for :users

  # Defines the root path route ("/")
  root 'main#welcome'

  # Route /portal to the main controller. This is a necessary workaround since 
  # Lookbook mimics requests from the root_path, which will return /portal.
  # However, to complete the mimicing, it uses the Rails.application.routes.recognize_path 
  # method, which does not work correctly for applications served on a subpath.
  match '/portal', to: 'main#welcome', via: :get

  resources :organizations, only: [:index, :show]

  if Rails.env.development?
    require 'sidekiq/web'
    mount Sidekiq::Web, at: '/sidekiq'
  end

  if Rails.env.development? || ENV["ENV"] == "dev"
    mount Lookbook::Engine, at: "lookbook"
  end
end
