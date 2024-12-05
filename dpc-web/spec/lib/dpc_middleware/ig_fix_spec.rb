# frozen_string_literal: true

require 'rails_helper'
require './lib/dpc_middleware/ig_fix'

describe DpcMiddleware::IgFix do
  let(:host) { 'http://example.org' }
  let(:path) { '/ig' }
  let(:app) { proc { [200, {}, ['Hello']] } }
  let(:middleware) { DpcMiddleware::IgFix.new(app) }
  let(:request) { Rack::MockRequest.new(middleware) }

  context 'without a query_string' do
    it 'appends / if a trailing / is not present and redirects' do
      response = request.get(path)
      expect(response.status).to eq(301)
      expect(response.headers['Location']).to eq(host + path + '/')
      expect(response.body).to be_empty
    end

    it 'does not redirect' do
      response = request.get(path + '/')
      expect(response.status).to eq(200)
      expect(response.body).to eq('Hello')
    end
  end

  context 'with a query_string' do
    it 'appends / if a trailing / is not present and redirects' do
      response = request.get(path + '?a=b')
      expect(response.status).to eq(301)
      expect(response.headers['Location']).to eq(host + path + '/?a=b')
      expect(response.body).to be_empty
    end

    it 'does not redirect' do
      response = request.get(path + '/?a=b')
      expect(response.status).to eq(200)
      expect(response.body).to eq('Hello')
    end
  end

  context 'with a different path' do
    it 'does not redirect' do
      response = request.get('/docs/guide')
      expect(response.status).to eq(200)
    end
  end
end
