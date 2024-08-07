# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :request, js: true, accessibility: true do
  include DpcClientSupport

  context 'not signed in' do
    before { WebMock.allow_net_connect! }
    after { WebMock.disable_net_connect! }

    it 'is accessible' do
      visit '/portal/users/sign_in'
      expect(page).to be_axe_clean
    end
  end
end
