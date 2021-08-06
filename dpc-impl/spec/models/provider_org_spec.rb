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
    scenario 'view list of provider orgs' do
      stub_api_client(message: :create_implementer, success: true, response: default_imp_creation_response)

      user = create(:user)

      sign_in user

      api_client = instance_double(ApiClient)
      allow(ApiClient).to receive(:new).and_return(api_client)
      allow(api_client).to receive(:get_provider_orgs)
        .with(user.implementer_id)
        .and_return(api_client)
      allow(api_client).to receive(:response_successful?).and_return(true)
      allow(api_client).to receive(:response_body).and_return(default_provider_orgs_list)

      visit root_path

      first_org = default_provider_orgs_list.first
      second_org = default_provider_orgs_list.second
      third_org = default_provider_orgs_list.third

      expect(page.body).to have_content(first_org[:org_name])
      expect(page.body).to have_content(second_org[:org_name])
      expect(page.body).to have_content(third_org[:org_name])
    end
  end

  describe 'could not connect to API' do
    scenario 'unable to add new or view existing provider org' do
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