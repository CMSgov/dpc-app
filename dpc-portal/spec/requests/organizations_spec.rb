# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Organizations', type: :request do
  include DpcClientSupport

  describe 'GET /index' do
    context 'not logged in' do
      it 'redirects to login' do
        get '/organizations'
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    describe 'logged in' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }
      before { sign_in user }

      it 'returns success if no orgs associated with user' do
        get '/organizations'
        expect(assigns(:links)).to be_empty
      end

      it 'returns organizations linked to user as ao' do
        link = create(:ao_org_link, provider_organization: org, user:)
        get '/organizations'
        expect(assigns(:links)).to eq [link]
      end

      it 'returns organizations linked to user as cd' do
        link = create(:cd_org_link, provider_organization: org, user:)
        get '/organizations'
        expect(assigns(:links)).to eq [link]
      end

      it 'logs user_id to new relic' do
        expect(NewRelic::Agent).to receive(:add_custom_attributes).with({ user_id: user.id })
        get '/organizations'
      end
    end

    context 'user has sanctions' do
      let!(:user) { create(:user, verification_status: 'rejected', verification_reason: 'ao_med_sanctions') }
      let!(:org) { create(:provider_organization) }
      before { sign_in user }

      it 'should show access denied page' do
        create(:ao_org_link, provider_organization: org, user:)
        get '/organizations'
        expect(response.body).to include(I18n.t('verification.ao_med_sanctions_status'))
        expect(assigns(:organizations)).to be_nil
      end
    end

    describe 'timed out' do
      let!(:user) { create(:user) }
      before { sign_in user }
      after { Timecop.return }

      it 'redirects to login after inactivity' do
        get '/organizations'
        expect(response.body).to include("<p>You don't have any organizations to show.</p>")
        Timecop.travel(30.minutes.from_now)
        get '/organizations'
        expect(response).to redirect_to('/portal/users/sign_in')
        expect(flash[:notice] = 'Your session expired. Please sign in again to continue.')
      end

      it 'redirects to login after session time elapses' do
        logged_in_at = Time.now
        get '/organizations'
        expect(response.body).to include("<p>You don't have any organizations to show.</p>")
        Timecop.scale(360) do # 1 real second = 1 simulated hour
          until Time.now > logged_in_at + 12.hours
            get '/organizations'
            expect(response.body).to include("<p>You don't have any organizations to show.</p>")
            Timecop.travel(20.minutes.from_now)
          end
          get '/organizations'
          expect(response).to redirect_to('/users/sign_in')
          expect(flash[:notice] = 'You have exceeded the maximum session length. Please sign in again to continue.')
        end
      end
    end
  end

  describe 'GET /organizations/[organization_id]' do
    context 'not logged in' do
      it 'redirects to login' do
        org = create(:provider_organization)
        get "/organizations/#{org.id}"
        expect(response).to redirect_to('/portal/users/sign_in')
      end
    end

    context 'no link to org' do
      let!(:user) { create(:user) }
      before { sign_in user }
      it 'redirects to organizations page' do
        org = create(:provider_organization)
        get "/organizations/#{org.id}"
        expect(response).to redirect_to(organizations_path)
      end
    end

    context 'ao access denied' do
      context 'org has sanctions' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, verification_status: 'rejected',
                                         verification_reason: 'org_med_sanctions')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}"
          expect(response.body).to include(I18n.t('verification.org_med_sanctions_status'))
        end
      end

      context 'org not approved' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}"
          expect(response.body).to include(I18n.t('verification.no_approved_enrollment_status'))
        end
      end

      context 'user no longer ao' do
        let!(:user) { create(:user) }
        let!(:org) { create(:provider_organization) }
        before { sign_in user }

        it 'should show access denied page' do
          create(:ao_org_link, provider_organization: org, user:, verification_status: false,
                               verification_reason: 'user_not_authorized_official')
          get "/organizations/#{org.id}"
          expect(response.body).to include(I18n.t('verification.user_not_authorized_official_status'))
        end
      end
    end
    context 'cd access denied' do
      context 'org has sanctions' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, verification_status: 'rejected',
                                         verification_reason: 'org_med_sanctions')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:cd_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}"
          expect(response.body).to include(I18n.t('cd_access.org_med_sanctions_status'))
        end
      end

      context 'org not approved' do
        let!(:user) { create(:user) }
        let!(:org) do
          create(:provider_organization, verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
        end
        before { sign_in user }

        it 'should show access denied page' do
          create(:cd_org_link, provider_organization: org, user:)
          get "/organizations/#{org.id}"
          expect(response.body).to include(I18n.t('cd_access.no_approved_enrollment_status'))
        end
      end
    end

    context 'as cd' do
      let!(:user) { create(:user) }
      let!(:org) { create(:provider_organization) }
      let!(:link) { create(:cd_org_link, user:, provider_organization: org) }
      before { sign_in user }

      context :not_signed_tos do
        it 'should redirect' do
          get "/organizations/#{org.id}"
          expect(response).to redirect_to(organizations_url)
        end
      end

      context :signed_tos do
        before { org.update(terms_of_service_accepted_by: user) }

        it 'returns success' do
          get "/organizations/#{org.id}"
          expect(response).to be_ok
          expect(assigns(:organization)).to eq org
        end

        it 'shows credential page' do
          get "/organizations/#{org.id}"
          expect(response.body).to include('<h2>Client Tokens</h2>')
          expect(response.body).to include('<h2>Public Keys</h2>')
          expect(response.body).to include('<h2>Public IPs</h2>')
        end

        it 'does not show CD list page' do
          get "/organizations/#{org.id}"
          expect(response.body).to_not include('<h2>Credential delegates</h2>')
          expect(response.body).to_not include('<h2>Pending</h2>')
          expect(response.body).to_not include('<h2>Active</h2>')
        end

        it 'does not assign invitations even if exist' do
          create(:invitation, :cd, provider_organization: org, invited_by: user)
          get "/organizations/#{org.id}"
          expect(assigns(:pending_invitations)).to be_nil
        end
      end
    end

    context 'as ao' do
      let!(:user) { create(:user) }
      before do
        create(:ao_org_link, user:, provider_organization: org)
        sign_in user
      end

      context :not_signed_tos do
        let!(:org) { create(:provider_organization) }
        it 'returns success' do
          get "/organizations/#{org.id}"
          expect(response).to be_ok
          expect(assigns(:organization)).to eq org
        end

        it 'shows tos page' do
          get "/organizations/#{org.id}"
          expect(response).to be_ok
          expect(response.body).to include('<h2>Sign Terms of Service</h2>')
        end
      end

      context :signed_tos do
        let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }
        it 'returns success' do
          get "/organizations/#{org.id}"
          expect(response).to be_ok
          expect(assigns(:organization)).to eq org
        end

        it 'shows CD list page' do
          get "/organizations/#{org.id}"
          expect(response.body).to include('<h2>Credential delegates</h2>')
          expect(response.body).to include('<h2>Pending invitations</h2>')
          expect(response.body).to include('<h2>Active</h2>')
          expect(response.body).to include('<h2>Expired invitations</h2>')
        end

        context :pending_invitations do
          it 'assigns if exist' do
            create(:invitation, :cd, provider_organization: org, invited_by: user)
            get "/organizations/#{org.id}"
            expect(assigns(:pending_invitations).size).to eq 1
          end

          it 'does not assign if not exist' do
            get "/organizations/#{org.id}"
            expect(assigns(:pending_invitations).size).to eq 0
          end

          it 'does not assign if only accepted exists' do
            create(:invitation, :cd, provider_organization: org, invited_by: user, status: :accepted)
            get "/organizations/#{org.id}"
            expect(assigns(:pending_invitations).size).to eq 0
          end

          it 'does not assign if expired' do
            create(:invitation, :cd, provider_organization: org, invited_by: user, created_at: 3.days.ago)
            get "/organizations/#{org.id}"
            expect(assigns(:pending_invitations).size).to eq 0
          end
        end

        context :expired_invitations do
          it 'assigns if exist' do
            create(:invitation, :cd, provider_organization: org, invited_by: user, created_at: 3.days.ago)
            get "/organizations/#{org.id}"
            expect(assigns(:expired_invitations).size).to eq 1
          end

          it 'does not assign if not exist' do
            get "/organizations/#{org.id}"
            expect(assigns(:pending_invitations).size).to eq 0
          end

          it 'does not assign if invitation is not expired' do
            create(:invitation, :cd, provider_organization: org, invited_by: user)
            get "/organizations/#{org.id}"
            expect(assigns(:expired_invitations).size).to eq 0
          end
        end

        context :credential_delegates do
          it 'assigns if exist' do
            create(:cd_org_link, provider_organization: org)
            get "/organizations/#{org.id}"
            expect(assigns(:cds).size).to eq 1
          end

          it 'does not assign if not exist' do
            get "/organizations/#{org.id}"
            expect(assigns(:cds).size).to eq 0
          end

          it 'does not assign if link disabled' do
            create(:cd_org_link, provider_organization: org, disabled_at: 1.day.ago)
            get "/organizations/#{org.id}"
            expect(assigns(:cds).size).to eq 0
          end
        end
      end
    end
  end

  describe 'AO org flow' do
    let!(:user) { create(:user) }
    before { sign_in user }

    context 'GET /organizations/new' do
      it 'returns success' do
        SecureRandom.uuid
        get '/organizations/new'
        expect(response).to be_ok
      end
    end

    context 'POST /organizations' do
      context 'with valid input' do
        it 'creates new org if none exists' do
          npi = '1111111111'
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 1
          org = assigns(:organization)
          expect(org.npi).to eq npi
          expect(org.name).to eq "Organization #{npi}"
          expect(org.terms_of_service_accepted_by).to be_nil
          expect(org.terms_of_service_accepted_at).to be_nil
          expect(response).to redirect_to(tos_form_organization_path(org))
        end

        it 'creates new ao-org-link if none exists' do
          npi = '1111111111'
          expect do
            post '/organizations', params: { npi: }
          end.to change { AoOrgLink.count }.by 1
          link = assigns(:ao_org_link)
          expect(link.provider_organization).to eq assigns(:organization)
          expect(link.user).to eq user
        end

        it 'does not create new org if exists' do
          npi = '1111111111'
          name = 'Health Hut'
          create(:provider_organization, npi:, name:)
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 0
          org = assigns(:organization)
          expect(org.npi).to eq npi
          expect(org.name).to eq name
          expect(org.terms_of_service_accepted_by).to be_nil
          expect(org.terms_of_service_accepted_at).to be_nil
          expect(response).to redirect_to(tos_form_organization_path(org))
        end

        it 'redirects to success if org has signed tos' do
          npi = '1111111111'
          create(:provider_organization, npi:, terms_of_service_accepted_at: 1.day.ago)
          expect do
            post '/organizations', params: { npi: }
          end.to change { ProviderOrganization.count }.by 0
          org = assigns(:organization)
          expect(response).to redirect_to(success_organization_path(org))
        end
      end

      it 'fails if blank' do
        post '/organizations', params: { npi: '' }
        expect(response).to be_bad_request
        expect(assigns(:npi_error)).to eq "can't be blank"
      end

      it 'fails if not 10 digits' do
        post '/organizations', params: { npi: '22' }
        expect(response).to be_bad_request
        expect(assigns(:npi_error)).to eq 'length has to be 10'
      end

      it 'fails ao_org_link error' do
        npi = '1111111111'
        failed_link = build(:ao_org_link)
        failed_link.errors.add(:base, 'Bad Link')
        ao_org_link_double = class_double(AoOrgLink).as_stubbed_const
        expect(ao_org_link_double).to receive(:find_or_create_by).and_return(failed_link)
        post '/organizations', params: { npi: }
        expect(response).to redirect_to(organizations_path)
        expect(flash[:alert]).to eq('System Error: unable to create link')
      end
    end

    context 'GET /organizations/[organization_id]/tos_form' do
      it 'renders tos form' do
        org = create(:provider_organization)
        create(:ao_org_link, provider_organization: org, user:)
        get "/organizations/#{org.id}/tos_form"
        expect(response.body).to include('<h2>Sign Terms of Service</h2>')
        expect(response).to be_ok
      end

      it 'fails if no org' do
        get '/organizations/fake-org/tos_form'
        expect(response).to be_not_found
      end
    end

    context 'POST /organizations/[organization_id]/sign_tos' do
      it 'succeeds if ao' do
        org = create(:provider_organization)
        create(:ao_org_link, provider_organization: org, user:)
        post "/organizations/#{org.id}/sign_tos"
        org.reload
        expect(org.terms_of_service_accepted_at).to be_present
        expect(org.terms_of_service_accepted_by).to eq user
        expect(response).to redirect_to(organization_path(org))
      end

      it 'logs if successful' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['Authorized Official signed Terms of Service',
                                                     { actionContext: LoggingConstants::ActionContext::Registration,
                                                       actionType: LoggingConstants::ActionType::AoSignedToS }])
        org = create(:provider_organization)
        create(:ao_org_link, provider_organization: org, user:)
        post "/organizations/#{org.id}/sign_tos"
      end

      it 'fails if not ao' do
        org = create(:provider_organization)
        create(:cd_org_link, provider_organization: org, user:)
        post "/organizations/#{org.id}/sign_tos"
        expect(org.terms_of_service_accepted_at).to_not be_present
        expect(response).to redirect_to(organizations_path)
      end
    end

    context 'GET /organizations/[organization_id]/success' do
      it 'shows success page' do
        org = create(:provider_organization)
        create(:ao_org_link, provider_organization: org, user:)
        get "/organizations/#{org.id}/success"
        expect(response).to be_ok
      end

      it 'fails if no org' do
        get '/organizations/fake-org/success'
        expect(response).to be_not_found
      end
    end
  end
end
