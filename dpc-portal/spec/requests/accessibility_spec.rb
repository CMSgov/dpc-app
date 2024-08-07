# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: feature, js: true, accessibility: true do
  include DpcClientSupport

  context 'client tokens' do
    let(:terms_of_service_accepted_by) { create(:user) }
    let!(:org) { create(:provider_organization, terms_of_service_accepted_by:) }
    let!(:user) { create(:user) }

    before do
      create(:cd_org_link, provider_organization: org, user:)
      sign_in user
      WebMock.allow_net_connect!
    end

    after { WebMock.disable_net_connect! }

    it 'GET /new is accessible' do
      visit "/organizations/#{org.id}/client_tokens/new"
      expect(page).to be_axe_clean
    end
  end
end
