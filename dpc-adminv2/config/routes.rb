Rails.application.routes.draw do
  scope 'adminv2' do

    root to: 'implementers#index', via: :get

  end
end
