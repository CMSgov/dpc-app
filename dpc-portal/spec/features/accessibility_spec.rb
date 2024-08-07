# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :request, js: true, accessibility: true do
  WebMock.allow_net_connect!
  context 'not signed in' do
    it '/sign_in' do
      visit '/portal/users/sign_in'
      expect(page).to be_axe_clean
    end
  end
  WebMock.disable_net_connect!
end
