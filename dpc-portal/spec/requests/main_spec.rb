# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Mains', type: :request do
  describe 'GET root' do
    it 'succeeds' do
      get root_path
      expect(response).to be_successful
    end

    it 'returns hello, world' do
      get root_path
      expect(response.body).to include('Hello, World')
    end

    it 'succeeds on prod-sbx' do
      allow(ENV)
        .to receive(:fetch)
        .with('ENV', nil)
        .and_return('prod-sbx')
      get root_path
      expect(response).to be_successful
    end
  end
end
