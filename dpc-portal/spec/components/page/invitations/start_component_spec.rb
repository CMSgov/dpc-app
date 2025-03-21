# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::StartComponent, type: :component do
  let(:component) { described_class.new(invitation.provider_organization, invitation) }
  before { render_inline(component) }

  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    it 'should have step component at step 1' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '1'
    end

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

    it 'should include organization name and NPI' do
      expect(page).to have_text "#{invitation.provider_organization.name} (NPI #{invitation.provider_organization.npi})"
    end
  end

  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    it 'should have step component at step 1' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '1'
    end

    it 'should show start button' do
      button = page.find('.usa-button')
      expect(button.text).to eq 'Accept invitation'
    end

    it 'should include the correct musts' do
      within('.usa-icon-list') do
        list_items = all('li')
        expect(list_items[0]).to have_text('Verify your identity with Login.gov')
        expect(list_items[1]).to have_text('Use the same email address the invite was sent to')
        expect(list_items[2]).to have_text('Make sure the name you sign up with matches the one shown on this screen')
      end
    end

    it 'should go to confirm_cd page' do
      expected = "/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/confirm_cd"
      form = page.find('form')
      expect(form[:action]).to eq expected
    end

    it 'should include organization name and NPI' do
      expect(page).to have_text "#{invitation.provider_organization.name} (NPI #{invitation.provider_organization.npi})"
    end
  end
end
