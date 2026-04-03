# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationHelper, type: :helper do
  describe 'current_user' do
    let(:user) { create(:user) }
    it 'should return nil if not set' do
      expect(helper.current_user).to be_nil
    end
    it 'should return if set' do
      assign(:current_user, user)
      expect(helper.current_user).to eq user
    end
  end
  describe 'omniauth_authorize_path' do
    it 'should return path to service' do
      expect(omniauth_authorize_path(:foo)).to eq '/portal/auth/foo'
    end
  end
end
