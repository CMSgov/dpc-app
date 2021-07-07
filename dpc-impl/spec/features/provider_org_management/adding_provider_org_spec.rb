# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.feature 'adding provider organization' do
  include ApiClientSupport

  scenario 'luhnacy automatically generates new npis' do
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

    visit new_provider_orgs_path

    npi1 = find_field('provider_org_npi').value

    npi1_check = LuhnacyLib.validate_npi('80840' + npi1)

    expect(npi1_check).to eq(true)

    visit new_provider_orgs_path

    npi2 = find_field('provider_org_npi').value

    expect(npi2).to_not eq(npi1)

    npi2_check = LuhnacyLib.validate_npi('80840' + npi2)

    expect(npi2_check).to eq(true)
  end

  describe 'successful API request' do
    scenario 'adding provider org' do
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

  describe 'could not connect to API' do
    scenario 'unable to add new provider org' do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      user = create(:user)

      sign_in user

      api_client = instance_double(ApiClient)
      allow(ApiClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_provider_orgs)
        .with(user.implementer_id)
        .and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(false)
      allow(api_client).to receive(:response_body).and_return(nil)

      visit root_path

      expect(page.body).to have_content("We were unable to connect to DPC due to an internal error.")

      visit new_provider_orgs_path

      expect(page.body).to have_content("We were unable to connect to DPC due to an internal error.")
    end
  end
end