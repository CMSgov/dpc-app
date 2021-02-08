Rails.application.routes.draw do
  resources :users, only: [:index, :show, :edit, :update, :destroy] do
    collection { get :download }
  end

  root to: 'users#index', via: :get
end
