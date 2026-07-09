# frozen_string_literal: true

# AO access denied
RSpec.shared_examples 'ao access denied with user sanctions' do |provider, new_path|
  let!(:user) do
    create_user_with_csp(csp: provider, verification_status: 'rejected', verification_reason: 'ao_med_sanctions')
  end
  let!(:org) { create(:provider_organization, terms_of_service_accepted_by: user) }

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:ao_org_link, provider_organization: org, user:)
    get new_path.call(org)
    expect(response.body).to include(I18n.t('verification.ao_med_sanctions_status'))
    expect(assigns(:organization)).to be_nil
  end
end

RSpec.shared_examples 'ao access denied with org sanctions' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org) do
    create(:provider_organization, terms_of_service_accepted_by: user,
                                   verification_status: 'rejected',
                                   verification_reason: 'org_med_sanctions')
  end

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:ao_org_link, provider_organization: org, user:)
    get new_path.call(org)
    expect(response.body).to include(I18n.t('verification.org_med_sanctions_status'))
  end
end

RSpec.shared_examples 'ao access denied with org not approved' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org) do
    create(:provider_organization, terms_of_service_accepted_by: user,
                                   verification_status: 'rejected',
                                   verification_reason: 'no_approved_enrollment')
  end

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:ao_org_link, provider_organization: org, user:)
    get new_path.call(org)
    expect(response.body).to include(I18n.t('verification.no_approved_enrollment_status'))
  end
end

RSpec.shared_examples 'ao access denied user no longer ao' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org)  { create(:provider_organization, terms_of_service_accepted_by: user) }

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:ao_org_link, provider_organization: org, user:,
                         verification_status: false,
                         verification_reason: 'user_not_authorized_official')
    get new_path.call(org)
    expect(response.body).to include(I18n.t('verification.user_not_authorized_official_status'))
  end
end

# CD access denied

RSpec.shared_examples 'cd access denied with org sanctions' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org) do
    create(:provider_organization, terms_of_service_accepted_by: user,
                                   verification_status: 'rejected',
                                   verification_reason: 'org_med_sanctions')
  end

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:cd_org_link, provider_organization: org, user:)
    get new_path.call(org)
    expect(response.body).to include(I18n.t('cd_access.org_med_sanctions_status'))
  end
end

RSpec.shared_examples 'cd access denied with org not approved' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org) do
    create(:provider_organization, terms_of_service_accepted_by: user,
                                   verification_status: 'rejected',
                                   verification_reason: 'no_approved_enrollment')
  end

  before { sign_in user, csp: provider }

  it 'shows access denied page' do
    create(:cd_org_link, provider_organization: org, user:)
    get new_path.call(org)
    expect(response.body).to include(I18n.t('cd_access.no_approved_enrollment_status'))
  end
end

# GET /new

RSpec.shared_examples 'GET /new with no link to org' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org)  { create(:provider_organization, terms_of_service_accepted_by: user) }

  before { sign_in user, csp: provider }

  it 'redirects to organizations' do
    get new_path.call(org)
    expect(response).to redirect_to('/organizations')
  end
end

RSpec.shared_examples 'GET /new with unsigned tos' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org)  { create(:provider_organization) }

  before do
    create(:cd_org_link, provider_organization: org, user:)
    sign_in user, csp: provider
  end

  it 'redirects to organizations page' do
    get new_path.call(org)
    expect(assigns(:organization)).to eq org
    expect(response).to redirect_to(organizations_path)
  end
end

RSpec.shared_examples 'GET /new as cd returns success' do |provider, new_path|
  let!(:user) { create_user_with_csp(csp: provider) }
  let!(:org)  { create(:provider_organization, terms_of_service_accepted_by: user) }

  before do
    create(:cd_org_link, provider_organization: org, user:)
    sign_in user, csp: provider
  end

  it 'returns success' do
    get new_path.call(org)
    expect(assigns(:organization)).to eq org
    expect(response).to have_http_status(200)
  end
end
