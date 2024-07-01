# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::StartComponent, type: :component do
  let(:ao_invite) { create(:invitation, invitation_type: :authorized_official) }
  let(:component) { described_class.new(ao_invite.provider_organization, ao_invite) }
  before { render_inline(component) }

  describe 'basic page' do
    it 'should show start button' do
      button = page.find('.usa-button')
      expect(button.text).to eq 'Start'
    end

    it 'should go to accept page' do
      expected = "/portal/organizations/#{ao_invite.provider_organization.id}/invitations/#{ao_invite.id}/accept"
      form = page.find('form')
      expect(form[:action]).to eq expected
    end
  end
end
