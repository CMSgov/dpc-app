# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::RegisterComponent, type: :component do
  let(:component) { described_class.new(invitation.provider_organization, invitation) }
  before { render_inline(component) }

  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    it 'should post to register' do
      expected = "/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/register"
      form = page.find('form')
      expect(form[:action]).to eq expected
    end
  end
  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    it 'should have step component at step 3' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '3'
    end

    it 'should post to register' do
      expected = "/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/register"
      form = page.find('form')
      expect(form[:action]).to eq expected
    end
  end
end
