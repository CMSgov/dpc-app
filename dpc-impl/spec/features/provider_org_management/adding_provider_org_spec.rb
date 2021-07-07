# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.feature 'adding provider organization' do
  include ApiClientSupport

  scenario 'successful API request' do
    stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

    user = create(:user)

    sign_in user

    api_client = instance_double(ApiClient)
    allow(ApiClient).to receive(:new).and_return(api_client)
    allow(api_client).to receive(:get_provider_orgs)
      .with(user.implementer_id)
      .and_return(api_client)
    allow(api_client).to receive(:response_successful?).and_return(true)
    allow(api_client).to receive(:response_body).and_return([])

    visit root_path

    find('[data-test="add-provider-org-test"]').click

    expect(page.body).to have_content("Add Provider Organization")

    npi = find_field('provider_org_npi').value

    npi_check = LuhnacyLib.validate_npi('80840' + npi)

    expect(npi_check).to eq(true)

    allow(api_client).to receive(:create_provider_org)
      .with(user.implementer_id, npi)
      .and_return(api_client)

    find('[data-test="porg-form-submit"]').click

    expect(page.body).to have_content("DPC Account Home Page")
    expect(page.body).to have_content("Provider Organization added.")
  end
end