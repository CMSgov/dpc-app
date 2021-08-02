# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

RSpec.describe ProviderOrgsController, type: :controller do
  include ApiClientSupport

  describe 'callbacks' do
    describe '#add_provider_org' do
      before do
        # stub default value
        allow(ENV).to receive(:[]).and_call_original
      end

      it 'invokes ApiClient and returns the response body' do
        api_client = stub_api_client(
          message: :create_provider_org,
          success: true,
          response: default_provider_org_response
        )

        npi = LuhnacyLib.generate_npi

        expect(api_client).to have_received(:create_provider_org)
          .with(default_provider_org_response)
          .once
        binding.pry

        expect().to eq(default_provider_org_response)
      end
    end
  end
end