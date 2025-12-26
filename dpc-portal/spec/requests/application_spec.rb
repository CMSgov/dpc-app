# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Application', type: :request do
  include LoginSupport
  before(:all) do
    Rails.application.routes.disable_clear_and_finalize = true
    Rails.application.routes.draw do
      match '/test', to: 'test#index', via: :get
    end
  end

  let!(:user) { create(:user) }
  before { sign_in user }

  it 'sets cache control to no-store' do
    get '/test'
    expect(response.body).to eq('foo')
    expect(response.headers['cache-control']).to eq 'no-store'
  end

  it 'logs user_id to new relic' do
    expect(NewRelic::Agent).to receive(:add_custom_attributes).with({ user_id: user.id })
    get '/test'
    expect(response.body).to eq('foo')
  end

  describe 'timed out' do
    after { Timecop.return }
    it 'redirects to login after inactivity' do
      get '/test'
      expect(response.body).to eq('foo')
      Timecop.travel(31.minutes.from_now)
      get '/test'
      expect(response).to redirect_to('/portal/users/sign_in')
      expect(flash[:notice] = 'Your session expired. Please sign in again to continue.')
    end

    it 'redirects to login after session time elapses' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['User session timed out',
                                                   { actionContext: LoggingConstants::ActionContext::Authentication,
                                                     actionType: LoggingConstants::ActionType::SessionTimedOut }])
      logged_in_at = Time.now
      get '/test'
      expect(response.body).to eq('foo')
      until Time.now > logged_in_at + 12.hours
        get '/test'
        expect(response.body).to eq('foo')
        Timecop.travel(29.minutes.from_now)
      end
      get '/test'
      expect(response).to redirect_to('/users/sign_in')
      expect(flash[:notice] = 'You have exceeded the maximum session length. Please sign in again to continue.')
    end
  end
end

class TestController < ApplicationController
  def index
    render plain: :foo
  end
end
