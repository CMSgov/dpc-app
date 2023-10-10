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
      expect(response.body).to eq('Hello, World')
    end
  end
end
