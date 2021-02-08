Rails.application.routes.draw do
  resources :users, only: [:index, :show, :edit, :update, :destroy] do
    collection { get :download }
  end

  resources :organizations do
    resources :registered_organizations, only: [:new, :create, :edit, :update, :destroy] do
      match :enable_or_disable, via: [:get, :post, :update]
    end
    match :add_or_delete, via: [:get, :post, :delete]
  end

  resources :taggings, only: [:create, :destroy]
  resources :tags, only: [:index, :create, :destroy]

  root to: 'users#index', via: :get
end
