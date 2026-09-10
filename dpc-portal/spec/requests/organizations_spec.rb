# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'
require 'support/user_access_shared_examples'

RSpec.describe 'Organizations', type: :request do
  include DpcClientSupport
  include ComponentSupport
  include LoginSupport

  CspUtils::CODES_TO_DISPLAY.each do |provider, display_name|
    context "using #{display_name}" do
      describe 'GET /index' do
        context 'not logged in' do
          it 'redirects to login' do
            get '/organizations'
            expect(response).to redirect_to('/users/sign_in')
          end
        end

        context 'logged in' do
          let!(:user) { create_user_with_csp(csp: provider) }
          let!(:org) { create(:provider_organization) }
          before { sign_in user, csp: provider }

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
        end

        context 'user has sanctions' do
          let!(:user) do
            create_user_with_csp(csp: provider, given_name: 'John', family_name: 'Smith',
                                 verification_status: 'rejected', verification_reason: 'ao_med_sanctions')
          end

          let!(:org) { create(:provider_organization) }
          before { sign_in user, csp: provider }

          it 'should show access denied page' do
            create(:ao_org_link, provider_organization: org, user:)
            get '/organizations'
            expect(response.body).to include(I18n.t('verification.ao_med_sanctions_status'))
            expect(assigns(:organizations)).to be_nil
          end
        end
      end
      describe 'GET /organizations/[organization_id]' do
        new_path = ->(org) { "/organizations/#{org.id}" }
        context 'not logged in' do
          it 'redirects to login' do
            org = create(:provider_organization)
            get "/organizations/#{org.id}"
            expect(response).to redirect_to('/users/sign_in')
          end
        end

        context 'no link to org' do
          let!(:user) { create_user_with_csp(csp: provider) }
          before { sign_in user, csp: provider }
          it 'redirects to organizations page' do
            org = create(:provider_organization)
            get "/organizations/#{org.id}"
            expect(response).to redirect_to(organizations_path)
          end
        end
        context 'as cd' do
          let!(:user) { create_user_with_csp(csp: provider) }
          let!(:org) { create(:provider_organization) }
          let!(:link) { create(:cd_org_link, user:, provider_organization: org) }
          before { sign_in user, csp: provider }

          context :not_signed_tos do
            it 'should redirect' do
              get "/organizations/#{org.id}"
              expect(response).to redirect_to(organizations_url)
            end
          end

          context :signed_tos do
            before { org.update(terms_of_service_accepted_by: user, terms_of_service_accepted_at: Time.now) }

            it 'returns success' do
              get "/organizations/#{org.id}"
              expect(response).to be_ok
              expect(assigns(:organization)).to eq org
            end

            it 'shows credential page' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('<h2>Client tokens</h2>')
              expect(response.body).to include('<h2>Public keys</h2>')
              expect(response.body).to include('<h2>Public IP addresses</h2>')
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

            it 'shows correct status' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('Setup needed')
              expect(response.body).to include('#warning')
            end

            it 'shows correct role' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('Role:</span> Credential Delegate')
            end
          end
        end
        context 'as ao' do
          let!(:user) { create_user_with_csp(csp: provider) }
          let!(:org) { create(:provider_organization) }
          before do
            create(:ao_org_link, user:, provider_organization: org)
            sign_in user, csp: provider
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
              expect(response.body).to include('<h1>Sign Terms of Service</h1>')
            end
          end

          context :signed_tos do
            before { org.update(terms_of_service_accepted_by: user, terms_of_service_accepted_at: Time.now) }
            it 'returns success' do
              get "/organizations/#{org.id}"
              expect(response).to be_ok
              expect(assigns(:organization)).to eq org
            end

            it 'should start on cd tab by default' do
              get "/organizations/#{org.id}"
              expect(response).to be_ok
              expect(response.body).to include(' make_current(0);')
              expect(response.body).to_not include(' make_current(1);')
            end

            it 'should start on credentials tab if credential_start param' do
              get "/organizations/#{org.id}", params: { credential_start: true }
              expect(response).to be_ok
              expect(response.body).to_not include(' make_current(0);')
              expect(response.body).to include(' make_current(1);')
            end

            it 'shows CD list page' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('<h2>Credential Delegates</h2>')
              expect(response.body).to include('<h3>Pending invites</h3>')
              expect(response.body).to include('<h3>Expired invites</h3>')
            end

            it 'shows correct status' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('Setup needed')
              expect(response.body).to include('#warning')
            end

            it 'shows correct role' do
              get "/organizations/#{org.id}"
              expect(response.body).to include('Role:</span> Authorized Official')
            end

            context :pending_invitations do
              it 'assigns if exist' do
                create(:invitation, :cd, provider_organization: org, invited_by: user)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:pending].size).to eq 1
              end

              it 'does not assign if not exist' do
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:pending].size).to eq 0
              end

              it 'does not assign if only accepted exists' do
                create(:invitation, :cd, provider_organization: org, invited_by: user, status: :accepted)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:pending].size).to eq 0
              end

              it 'does not assign if expired' do
                create(:invitation, :cd, provider_organization: org, invited_by: user, created_at: 3.days.ago)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:pending].size).to eq 0
              end
            end

            context :expired_invitations do
              it 'assigns if exist' do
                create(:invitation, :cd, provider_organization: org, invited_by: user, created_at: 3.days.ago)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:expired].size).to eq 1
              end

              it 'does not assign if not exist' do
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:pending].size).to eq 0
              end

              it 'does not assign if invitation is not expired' do
                create(:invitation, :cd, provider_organization: org, invited_by: user)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:expired].size).to eq 0
              end
            end

            context :credential_delegates do
              it 'assigns if exist' do
                create(:cd_org_link, provider_organization: org)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:active].size).to eq 1
              end

              it 'does not assign if not exist' do
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:active].size).to eq 0
              end

              it 'does not assign if link disabled' do
                create(:cd_org_link, provider_organization: org, disabled_at: 1.day.ago)
                get "/organizations/#{org.id}"
                expect(assigns(:delegate_information)[:active].size).to eq 0
              end
            end
          end
        end

        context 'ao access denied' do
          context 'org has sanctions' do
            it_behaves_like 'ao access denied with org sanctions', provider, new_path
          end
          context 'org not approved' do
            it_behaves_like 'ao access denied with org not approved', provider, new_path
          end
          context 'user no longer ao' do
            it_behaves_like 'ao access denied user no longer ao', provider, new_path
          end
        end

        context 'cd access denied' do
          context 'org has sanctions' do
            it_behaves_like 'cd access denied with org sanctions', provider, new_path
          end
          context 'org not approved' do
            it_behaves_like 'cd access denied with org not approved', provider, new_path
          end
        end

        describe 'AO org flow' do
          let(:uuid) { SecureRandom.uuid }
          let!(:user) { create_user_with_csp(csp: provider, uuid: uuid) }
          before { sign_in user, csp: provider }

          context 'GET /organizations/[organization_id]/tos_form' do
            it 'renders tos form' do
              org = create(:provider_organization)
              create(:ao_org_link, provider_organization: org, user:)
              get "/organizations/#{org.id}/tos_form"
              expect(response.body).to include('<h1>Sign Terms of Service</h1>')
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
              org = create(:provider_organization)
              create(:ao_org_link, provider_organization: org, user:)
              allow(Rails.logger).to receive(:info)
              expect(Rails.logger).to receive(:info).with(['Authorized Official signed Terms of Service',
                                                           hash_including(actionContext: LoggingConstants::ActionContext::Registration,
                                                                          actionType: LoggingConstants::ActionType::AoSignedToS,
                                                                          user_identifier: uuid,
                                                                          csp: provider.to_s,
                                                                          organization_npi: org.npi)])
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
        end
      end
    end
  end
end
