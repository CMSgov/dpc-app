# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::AcceptInvitationComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }
    let(:ao) { build(:user, given_name: 'Bob', family_name: 'Hodges') }
    let(:cd_invite) { Invitation.new(id: 4, invited_by: ao, verification_code: 'ABC123') }

    let(:component) { described_class.new(org, cd_invite) }

    before do
      render_inline(component)
    end

    context 'New form' do
      it 'should match header' do
        header = <<~HTML
          <h1>Accept organization invite</h1>
            <div class="usa-alert usa-alert--info margin-bottom-4">
              <div class="usa-alert__body">
                <h4 class="usa-alert__heading">Key information</h4>
                <p class="usa-alert__text">
        HTML
        is_expected.to include(normalize_space(header))
      end

      it 'should match form tag' do
        form_url = "/portal/organizations/#{org.path_id}/credential_delegate_invitations/#{cd_invite.id}/confirm"
        form_tag = ['<form class="usa-form" id="cd-accept-form"',
                    %(action="#{form_url}"),
                    'accept-charset="UTF-8" method="post">'].join(' ')
        is_expected.to include(form_tag)
      end

      it 'should mention org name' do
        is_expected.to include(org.name)
      end

      it 'should mention AO name' do
        is_expected.to include('Bob Hodges')
      end

      it 'should have empty verification_code stanza' do
        verification_code = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="verification_code">Enter the invite code:</label>
            <input type="text" name="verification_code" id="verification_code" value="" maxlength="6" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(verification_code))
      end
    end

    context 'Errors' do
      let(:error_msg) { 'Some error message' }

      before { cd_invite.errors.add(:verification_code, :is_bad, message: error_msg) }
      it 'should have errored verification_code stanza' do
        verification_code = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="verification_code">Enter the invite code:</label>
            <p style="color: #b50909;">#{error_msg}</p>
            <input type="text" name="verification_code" id="verification_code" value="" maxlength="6" class="usa-input usa-input--error" />
          </div>
        HTML
        is_expected.to include(normalize_space(verification_code))
      end
    end
  end
end
