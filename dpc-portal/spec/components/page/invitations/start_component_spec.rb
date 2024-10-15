# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::StartComponent, type: :component do
  let(:component) { described_class.new(invitation.provider_organization, invitation) }
  before { render_inline(component) }

  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    it 'should show start button' do
      button = page.find('.usa-button')
      expect(button.text).to eq 'Begin registration'
    end

    it 'should include the correct musts' do
      within('.usa-icon-list') do
        list_items = all('li')
        expect(list_items[0]).to have_text('Be an active AO of your organization')
        expect(list_items[1]).to have_text('Not be listed on the Medicare Exclusions Database (or your organization)')
        expect(list_items[2]).to have_text(
          'Be registered in the Provider Enrollment, Chain, and Ownership System (PECOS)'
        )
      end
    end

    it 'should go to accept page' do
      expected = "/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/accept"
      form = page.find('form')
      expect(form[:action]).to eq expected
    end
  end
  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    it 'should show start button' do
      button = page.find('.usa-button')
      expect(button.text).to eq 'Confirm your identity'
    end

    it 'should include the correct musts' do
      within('.usa-icon-list') do
        list_items = all('li')
        expect(list_items[0]).to have_text('Verify your identity with Login.gov')
        expect(list_items[1]).to have_text('Enter your invite code')
      end
    end
  end
end
