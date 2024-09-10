# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::OtpComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }

    before do
      render_inline(component)
    end
    context 'credential delegate' do
      let(:ao) { build(:user, given_name: 'Bob', family_name: 'Hodges') }
      let(:cd_invite) do
        Invitation.new(id: 4, invited_by: ao, verification_code: 'ABC123', invitation_type: :credential_delegate)
      end
      let(:component) { described_class.new(org, cd_invite) }

      context 'New form' do
        it 'should match form tag' do
          form_url = "/portal/organizations/#{org.path_id}/invitations/#{cd_invite.id}/verify_code"
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
        let(:error_msg) { 'Incorrect invite code. You have 4 remaining attempts.' }

        before do
          cd_invite.update_attempts
          cd_invite.errors.add(:verification_code, :is_bad, message: error_msg)
        end
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
end
