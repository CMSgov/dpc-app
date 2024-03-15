# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::NewInvitationComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }
    let(:cd_invite) { Invitation.new }

    let(:component) { described_class.new(org, cd_invite) }

    before do
      render_inline(component)
    end

    context 'New form' do
      it 'should match header' do
        header = <<~HTML
          <h1>Invite new user</h1>
            <div class="usa-alert usa-alert--warning margin-bottom-4">
              <div class="usa-alert__body">
                <h4 class="usa-alert__heading">Exact match required</h4>
                <p class="usa-alert__text">
                  The name and contact info you enter must be an exact match to the name and contact info your Credential Delegate will provide after receiving this invite.
                </p>
              </div>
            </div>
          <div>
        HTML
        is_expected.to include(normalize_space(header))
      end

      it 'should match form tag' do
        form_tag = ['<form class="usa-form" id="cd-form"',
                    %(action="/portal/organizations/#{org.path_id}/credential_delegate_invitations"),
                    'accept-charset="UTF-8" method="post">'].join(' ')
        is_expected.to include(form_tag)
      end

      it 'should have empty given name stanza' do
        first_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_given_name">First or given name</label>
            <p class="usa-hint">For example, Jose, Darren, or Mai</p>
            <input type="text" name="invited_given_name" id="invited_given_name" maxlength="25" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(first_name))
      end

      it 'should have empty family name stanza' do
        invited_family_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_family_name">Last or family name</label>
            <p class="usa-hint">For example, Martinez Gonzalez, Gu, or Smith</p>
            <input type="text" name="invited_family_name" id="invited_family_name" maxlength="25" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(invited_family_name))
      end

      it 'should have empty phone stanza' do
        phone = ['<label class="usa-label" for="phone_raw">Primary phone number</label>',
                 '<p class="usa-hint">10-digit, U.S. only, for example 999-999-9999</p>',
                 '<input type="text" name="phone_raw" id="phone_raw" ',
                 'class="usa-input usa-masked" placeholder="___-___-____" ',
                 'pattern="\\d{3}-\\d{3}-\\d{4}" aria-describedby="telHint" />'].join
        is_expected.to include(phone)
      end

      it 'should have empty email stanza' do
        email = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email">Email</label>
            <input type="text" name="invited_email" id="invited_email" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(email))
      end

      it 'should have empty email confirmation stanza' do
        email_confirmation = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email_confirmation">Confirm email</label>
            <input type="text" name="invited_email_confirmation" id="invited_email_confirmation" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(email_confirmation))
      end

      it 'should have modal prompt' do
        modal_prompt = ['<a href="#verify-modal" aria-controls="verify-modal" ',
                        'class="usa-button" data-open-modal>',
                        'Send invite',
                        '</a>'].join
        is_expected.to include(modal_prompt)
      end
    end

    context 'Errors' do
      before { cd_invite.valid? }
      it 'should have errored given name stanza' do
        first_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_given_name">First or given name</label>
            <p class="usa-hint">For example, Jose, Darren, or Mai</p>
            <p style="color: #b50909;">can't be blank</p>
            <input type="text" name="invited_given_name" id="invited_given_name" maxlength="25" class="usa-input usa-input--error" />
          </div>
        HTML
        is_expected.to include(normalize_space(first_name))
      end

      it 'should have errored family name stanza' do
        invited_family_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_family_name">Last or family name</label>
            <p class="usa-hint">For example, Martinez Gonzalez, Gu, or Smith</p>
            <p style="color: #b50909;">can't be blank</p>
            <input type="text" name="invited_family_name" id="invited_family_name" maxlength="25" class="usa-input usa-input--error" />
          </div>
        HTML
        is_expected.to include(normalize_space(invited_family_name))
      end

      it 'should have errored phone stanza' do
        phone = ['<label class="usa-label" for="phone_raw">Primary phone number</label>',
                 '<p class="usa-hint">10-digit, U.S. only, for example 999-999-9999</p>',
                 %(<p style="color: #b50909;">can't be blank</p>),
                 '<input type="text" name="phone_raw" id="phone_raw" ',
                 'class="usa-input usa-masked usa-input--error" placeholder="___-___-____" ',
                 'pattern="\\d{3}-\\d{3}-\\d{4}" aria-describedby="telHint" />'].join
        is_expected.to include(normalize_space(phone))
      end

      it 'should have errored email stanza' do
        email = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email">Email</label>
            <p style="color: #b50909;">can't be blank</p>
            <input type="text" name="invited_email" id="invited_email" class="usa-input usa-input--error" />
          </div>
        HTML
        is_expected.to include(normalize_space(email))
      end

      it 'should have errored email confirmation stanza' do
        email_confirmation = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email_confirmation">Confirm email</label>
            <p style="color: #b50909;">can't be blank</p>
            <input type="text" name="invited_email_confirmation" id="invited_email_confirmation" class="usa-input usa-input--error" />
          </div>
        HTML
        is_expected.to include(normalize_space(email_confirmation))
      end
    end

    context 'Pre-filled' do
      before do
        cd_invite.invited_given_name = 'Bob'
        cd_invite.invited_family_name = 'Hodges'
        cd_invite.phone_raw = '222-222-2222'
        cd_invite.invited_email = cd_invite.invited_email_confirmation = 'bob@example.com'
      end

      it 'should have filled given name stanza' do
        first_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_given_name">First or given name</label>
            <p class="usa-hint">For example, Jose, Darren, or Mai</p>
            <input type="text" name="invited_given_name" id="invited_given_name" value="Bob" maxlength="25" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(first_name))
      end

      it 'should have filled family name stanza' do
        invited_family_name = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_family_name">Last or family name</label>
            <p class="usa-hint">For example, Martinez Gonzalez, Gu, or Smith</p>
            <input type="text" name="invited_family_name" id="invited_family_name" value="Hodges" maxlength="25" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(invited_family_name))
      end

      it 'should have filled phone stanza' do
        phone = ['<label class="usa-label" for="phone_raw">Primary phone number</label>',
                 '<p class="usa-hint">10-digit, U.S. only, for example 999-999-9999</p>',
                 '<input type="text" name="phone_raw" id="phone_raw" value="222-222-2222" ',
                 'class="usa-input usa-masked" placeholder="___-___-____" ',
                 'pattern="\\d{3}-\\d{3}-\\d{4}" aria-describedby="telHint" />'].join
        is_expected.to include(normalize_space(phone))
      end

      it 'should have filled email stanza' do
        email = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email">Email</label>
            <input type="text" name="invited_email" id="invited_email" value="bob@example.com" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(email))
      end

      it 'should have filled email confirmation stanza' do
        email_confirmation = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="invited_email_confirmation">Confirm email</label>
            <input type="text" name="invited_email_confirmation" id="invited_email_confirmation" value="bob@example.com" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(email_confirmation))
      end
    end
  end
end
