# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Application', type: :request do
  before(:all) do
    Rails.application.routes.disable_clear_and_finalize = true
    Rails.application.routes.draw do
      match '/test', to: 'test#index', via: :get
    end
  end

  it 'sets cache control to no-store' do
    get '/test'
    expect(response.headers['cache-control']).to eq 'no-store'
  end
end

class TestController < ApplicationController
  def index
    render plain: 'foo'
  end
end
