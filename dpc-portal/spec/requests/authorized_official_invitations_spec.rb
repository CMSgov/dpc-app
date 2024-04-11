require 'rails_helper'

RSpec.describe "AuthorizedOfficialInvitations", type: :request do
  describe 'GET /accept' do
    let!(:ao_invite) { create(:invitation, :ao) }
    let(:org) { create(:provider_organization) }
    it 'should show not implemented' do
      get "/organizations/#{org.id}/authorized_official_invitations/#{ao_invite.id}/accept"
      expect(response.status).to eq(501)
    end
  end
end
