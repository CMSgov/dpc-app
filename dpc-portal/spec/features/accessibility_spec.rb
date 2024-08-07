# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :request, js: true, accessibility: true do
  context 'not signed in' do
    it '/sign_in' do
      WebMock.allow_net_connect!
      visit '/portal/users/sign_in'
      expect(page).to be_axe_clean
      WebMock.disable_net_connect!
    end
  end
end
