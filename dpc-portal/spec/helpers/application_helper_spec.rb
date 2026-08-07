# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationHelper, type: :helper do
  describe 'omniauth_authorize_path' do
    it 'should return path to service' do
      expect(omniauth_authorize_path(:foo)).to eq '/auth/foo'
    end
  end
end
