# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::AcceptInvitationComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }
    let(:given_name) { 'Paola' }
    let(:family_name) { 'Pineiro' }

    before do
      render_inline(component)
    end
    context 'authorized official' do
      let(:ao_invite) { Invitation.new(id: 5, invitation_type: :authorized_official) }
      let(:component) { described_class.new(org, ao_invite, given_name, family_name) }
      it 'should match form method and action' do
        form_url = "/portal/organizations/#{org.path_id}/invitations/#{ao_invite.id}/confirm"
        form_method_action = %(method="post" action="#{form_url}")
        is_expected.to include(form_method_action)
      end

      it 'should have step component at step 3' do
        expect(page).to have_selector('.usa-step-indicator__current-step')
        expect(page.find('.usa-step-indicator__current-step').text).to eq '3'
      end

      it 'should have Continue to register button' do
        is_expected.to include('Continue to register')
      end
    end

    context 'credential delegate' do
      let(:cd_invite) { Invitation.new(id: 6, invitation_type: :credential_delegate) }
      let(:component) { described_class.new(org, cd_invite, given_name, family_name) }
      it 'should match form method and action' do
        form_url = "/portal/organizations/#{org.path_id}/invitations/#{cd_invite.id}/register"
        form_method_action = %(method="post" action="#{form_url}")
        is_expected.to include(form_method_action)
      end

      it 'should have step component at step 3' do
        expect(page).to have_selector('.usa-step-indicator__current-step')
        expect(page.find('.usa-step-indicator__current-step').text).to eq '3'
      end

      it 'should have Accept invite button' do
        is_expected.to include('Accept invite')
      end
    end
  end
end
