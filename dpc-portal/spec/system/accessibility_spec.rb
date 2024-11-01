# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :system do
  include Devise::Test::IntegrationHelpers
  include DpcClientSupport
  before do
    driven_by(:selenium_headless)
  end
  context do
    it 'shows login page ok' do
      visit '/users/sign_in'
      expect(page).to_not have_text('You need to sign in or sign up before continuing.')
      expect(page).to have_text('Sign in')
      expect(page).to be_axe_clean
    end
  end

  context do
    let(:dpc_api_organization_id) { 'some-gnarly-guid' }
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let(:mock_ctm) { instance_double(ClientTokenManager) }
    let(:mock_pkm) { instance_double(PublicKeyManager) }
    let(:mock_iam) { instance_double(IpAddressManager) }
    let(:tokens) { [] }
    let(:keys) { [] }
    let(:ip_addresses) { [] }

    before do
      allow(ClientTokenManager).to receive(:new).and_return(mock_ctm)
      allow(PublicKeyManager).to receive(:new).and_return(mock_pkm)
      allow(IpAddressManager).to receive(:new).and_return(mock_iam)
      allow(mock_ctm).to receive(:client_tokens).and_return(tokens)
      allow(mock_pkm).to receive(:public_keys).and_return(keys)
      allow(mock_iam).to receive(:ip_addresses).and_return(ip_addresses)
      sign_in user
    end
    it 'organizations with empty list' do
      visit '/organizations'
      expect(page).to have_text("You don't have any organizations to show.")
      expect(page).to be_axe_clean
    end
    context :with_organizations do
      let!(:ao_org_link) { create(:ao_org_link, user:, provider_organization: org) }
      it 'should show organizations' do
        visit '/organizations'
        expect(page).to_not have_text("You don't have any organizations to show.")
        expect(page).to be_axe_clean
      end
      it 'should show tos' do
        visit "/organizations/#{org.id}"
        expect(page).to have_text('Terms of Service')
        expect(page).to_not have_text('You can assign anyone as a CD')
        expect(page).to be_axe_clean
      end
      it 'can sign tos' do
        visit "/organizations/#{org.id}"
        page.find('.usa-button', text: 'I have read and accepted the Terms of Service').click
        expect(page).to_not have_text('Terms of Service')
        expect(page).to have_text('You can assign anyone as a CD')
        expect(page).to be_axe_clean
      end
      context :after_tos do
        before { org.update!(terms_of_service_accepted_by: user) }
        it 'should show organization page with no cds' do
          visit "/organizations/#{org.id}"
          expect(page).to have_text('You can assign anyone as a CD')
          expect(page).to have_css('#credential_delegates')
          expect(page).to_not have_css('#credentials')
          expect(page).to_not have_css('#active-cd-table')
          expect(page).to_not have_css('#pending-cd-table')
          expect(page).to be_axe_clean
        end
        it 'should show organization page with no credentials' do
          visit "/organizations/#{org.id}"
          page.execute_script('make_current(1)')
          expect(page).to have_text('you must create a unique client token')
          expect(page).to_not have_css('#credential_delegates')
          expect(page).to have_css('#credentials')
          expect(page).to_not have_css('#client-tokens-table')
          expect(page).to_not have_css('#public-keys-table')
          expect(page).to_not have_css('#public-ips-table')
          expect(page).to be_axe_clean
        end
        context :with_credential_delegates do
          let(:active_cd) { create(:user) }
          let!(:active_link) { create(:cd_org_link, user: active_cd, provider_organization: org) }
          let!(:invitation) { create(:invitation, :cd, provider_organization: org, invited_by: user) }
          it 'should show credential delegate tables' do
            visit "/organizations/#{org.id}"
            expect(page).to have_text('You can assign anyone as a CD')
            expect(page).to have_css('#active-cd-table')
            expect(page).to have_css('#pending-cd-table')
            expect(page).to be_axe_clean
          end
        end
        context :with_credentials do
          let(:tokens) { default_get_client_tokens['entities'] }
          let(:keys) { default_get_public_keys['entities'] }
          let(:ip_addresses) { default_get_ip_addresses['entities'] }
          it 'should show credentials' do
            visit "/organizations/#{org.id}"
            page.execute_script('make_current(1)')
            expect(page).to have_css('#client-tokens-table')
            expect(page).to have_css('#public-keys-table')
            expect(page).to have_css('#public-ips-table')
            expect(page).to be_axe_clean
          end
        end
        context :client_tokens do
          let(:tokens) { default_get_client_tokens['entities'] }
          it 'should show new page' do
            visit "/organizations/#{org.id}/client_tokens/new"
            expect(page).to have_text('Create a new client token')
            expect(page).to_not have_text('Label required')
            expect(page).to be_axe_clean
          end
          it 'should show error page' do
            expect(mock_ctm).to receive(:create_client_token).and_return({})
            expect(mock_ctm).to receive(:errors).and_return({ root: 'Test Error' })
            visit "/organizations/#{org.id}/client_tokens/new"
            page.all('.usa-button')[1].click
            expect(page).to have_text('Test Error')
            expect(page).to be_axe_clean
          end
          it 'should show success page' do
            expect(mock_ctm).to receive(:create_client_token).and_return({ response: true, message: :token })
            visit "/organizations/#{org.id}/client_tokens/new"
            page.fill_in 'label', with: 'new token'
            page.all('.usa-button')[1].click
            expect(page).to have_text('Client token created')
            expect(page).to be_axe_clean
          end
        end
        context :public_keys do
          let(:keys) { default_get_public_keys['entities'] }
          it 'should show new page' do
            visit "/organizations/#{org.id}/public_keys/new"
            expect(page).to have_text('Add public key')
            expect(page).to_not have_text('Required values missing')
            expect(page).to be_axe_clean
          end
          it 'should show error page' do
            expect(mock_pkm).to receive(:create_public_key).and_return({ errors: { root: 'Test Error' } })
            visit "/organizations/#{org.id}/public_keys/new"
            page.all('.usa-button')[1].click
            expect(page).to have_text('Test Error')
            expect(page).to be_axe_clean
          end
          it 'should show success page' do
            expect(mock_pkm).to receive(:create_public_key).and_return({ response: { message: { 'id' => 'foo' } } })
            visit "/organizations/#{org.id}/public_keys/new"
            page.fill_in 'label', with: 'new key'
            page.fill_in 'public_key', with: 'key'
            page.all('.usa-button')[1].click
            expect(page).to have_text('Public key successfully created')
            expect(page).to be_axe_clean
          end
        end
        context :ip_addresses do
          let(:keys) { default_get_ip_addresses['entities'] }
          it 'should show new page' do
            visit "/organizations/#{org.id}/ip_addresses/new"
            expect(page).to have_text('Add public IP address')
            expect(page).to_not have_text('Label required')
            expect(page).to be_axe_clean
          end
          it 'should show error page' do
            expect(mock_iam).to receive(:create_ip_address).and_return({ errors: { root: 'Test Error' } })
            visit "/organizations/#{org.id}/ip_addresses/new"
            page.find_button(value: 'Add IP').click
            expect(page).to have_text('Test Error')
            expect(page).to be_axe_clean
          end
          it 'should show success page' do
            expect(mock_iam).to receive(:create_ip_address).and_return({ response: { message: { 'id' => 'foo' } } })
            visit "/organizations/#{org.id}/ip_addresses/new"
            page.find_button(value: 'Add IP').click
            expect(page).to have_text('IP address successfully created')
            expect(page).to be_axe_clean
          end
        end
        context :credential_delegate_invitation do
          it 'should show new page' do
            visit "/organizations/#{org.id}/credential_delegate_invitations/new"
            expect(page).to have_text('Send invite')
            expect(page).to_not have_text("can't be blank")
            expect(page).to be_axe_clean
          end
          it 'should show error page' do
            visit "/organizations/#{org.id}/credential_delegate_invitations/new"
            page.find('.usa-button', text: 'Send invite').click
            page.find_button(value: 'Yes, I acknowledge').click
            expect(page).to have_text("can't be blank")
            expect(page).to be_axe_clean
          end
          it 'should show success page' do
            visit "/organizations/#{org.id}/credential_delegate_invitations/new"
            page.fill_in 'invited_given_name', with: 'John'
            page.fill_in 'invited_family_name', with: 'Lennon'
            page.fill_in 'invited_email', with: 'john@beatles.com'
            page.fill_in 'invited_email_confirmation', with: 'john@beatles.com'
            page.find('.usa-button', text: 'Send invite').click
            page.find_button(value: 'Yes, I acknowledge').click
            expect(page).to_not have_text("can't be blank")
            expect(page).to have_text('Credential delegate invite sent')
            expect(page).to be_axe_clean
          end
        end
      end
    end
    context :ao_invitation_flow do
      let!(:invitation) { create(:invitation, :ao, provider_organization: org) }
      let!(:mock_uis) { instance_double(UserInfoService) }
      let(:user_info) do
        {
          'sub' => 'some-guid',
          'all_emails' => [invitation.invited_email],
          'email' => invitation.invited_email,
          'social_security_number' => '900111111'
        }
      end
      before do
        allow(UserInfoService).to receive(:new).and_return(mock_uis)
        allow(mock_uis).to receive(:user_info).and_return(user_info)
      end
      it 'should show intro page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        expect(page).to have_text('Not be listed on the Medicare Exclusions Database')
        expect(page).to be_axe_clean
      end
      it 'should show login page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        expect(page).to have_text('Sign in or create')
        expect(page).to be_axe_clean
      end
      it 'should show accept page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        expect(page).to have_text('Step 2')
        expect(page).to be_axe_clean
      end
      it 'should show confirm page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        page.find('.usa-button', text: 'Continue to register').click
        expect(page).to have_text('Step 3')
        expect(page).to be_axe_clean
      end
      it 'should show register page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
        page.find('.usa-button', text: 'Continue to register').click
        page.find('.usa-button', text: 'Complete registration').click
        expect(page).to have_text('Step 4')
        expect(page).to be_axe_clean
      end
      context :failure do
        let(:user_info) do
          {
            'sub' => 'some-guid',
            'all_emails' => [invitation.invited_email],
            'email' => invitation.invited_email,
            'social_security_number' => '900111112'
          }
        end
        it 'bad invitation' do
          visit "/organizations/#{org.id}/invitations/bad-id"
          expect(page).to have_text('Your link is invalid.')
          expect(page).to be_axe_clean
        end
        it 'expired invitation' do
          expired_invitation = create(:invitation, :ao, provider_organization: org, created_at: 4.days.ago)
          visit "/organizations/#{org.id}/invitations/#{expired_invitation.id}"
          expect(page).to have_text('Your registration link has expired.')
          expect(page).to be_axe_clean
        end
        it 'email does not match' do
          mismatched_invitation = create(:invitation, :ao, provider_organization: org,
                                                           invited_email: 'somethingelse@example.com',
                                                           invited_email_confirmation: 'somethingelse@example.com')
          visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/set_idp_token"
          visit "/organizations/#{org.id}/invitations/#{mismatched_invitation.id}/accept"
          expect(page).to have_text("The email you used to sign in doesn't match your invite.")
          expect(page).to be_axe_clean
        end
        it 'should show fail ao check' do
          visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
          visit "/organizations/#{org.id}/invitations/#{invitation.id}/accept"
          page.find('.usa-button', text: 'Continue to register').click
          expect(page).to have_text('Step 3')
          expect(page).to have_text('You’re not the Authorized Official.')
          expect(page).to be_axe_clean
        end
      end
    end
    context :cd_invitation_flow do
      let!(:invitation) { create(:invitation, :cd, provider_organization: org) }
      let!(:mock_uis) { instance_double(UserInfoService) }
      let(:user_info) do
        {
          'sub' => 'some-guid',
          'all_emails' => [invitation.invited_email],
          'email' => invitation.invited_email,
          'given_name' => invitation.invited_given_name,
          'family_name' => invitation.invited_family_name
        }
      end
      before do
        allow(UserInfoService).to receive(:new).and_return(mock_uis)
        allow(mock_uis).to receive(:user_info).and_return(user_info)
      end
      it 'should show intro page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}"
        expect(page).to have_text("You've been delegated to manage access")
        expect(page).to be_axe_clean
      end
      it 'should show login page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
        expect(page).to have_text('Sign in or create a Login.gov account')
        expect(page).to be_axe_clean
      end
      it 'should show confirm page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
        expect(page).to have_text('Accept invite')
        expect(page).to be_axe_clean
      end
      it 'should show success page' do
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/set_idp_token"
        visit "/organizations/#{org.id}/invitations/#{invitation.id}/confirm_cd"
        page.find('.usa-button', text: 'Accept invite').click
        expect(page).to have_text('Thank you for accepting your invite.')
        expect(page).to be_axe_clean
      end
    end
  end
end
