# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PublicKeyManager do

  describe '#create_system' do
    context 'with valid key and signature' do
      before(:each) do
        user = create(:user)
        @imp_id = user.implementer_id
        @org_id = "77233b56-555c-4c29-8d29-dc7c615c8c31"
      end

      it 'API responds true' do
        api_client = instance_double(ApiClient)
        allow(ApiClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:create_system)
          .with(@imp_id, '77233b56-555c-4c29-8d29-dc7c615c8c31', params: {:client_name=>"Org Name System",
            :public_key=>file_fixture('stubbed_key.pem').read,
            :signature=>file_fixture('stubbed_signature.pem').read})
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return({:client_id=>@org_id,
          :client_name=>"Org Name",
          :client_token=>
            "MDAxN2xvY2F0aW9uIGxvY2FsaG9zdAowMDM0aWRlbnRpZmllciA5OWQ4M2M1YS1mMmRiLTRlYmEtODhkZS1jYmM3OTY0ZTA3MzEKMDAyOGNpZCBleHBpcmF0aW9uPTIwMjItMDgtMjZUMTk6MjA6MzNaCjAwMTRjaWQgc3lzdGVtX2lkPTUKMDA2MGNpZCBncm91cF9kYXRhPWV5SnBiWEJzWlcxbGJuUmxja2xFSWpvZ0lqTXdNRGhpWkRnMExUTTBaR010TkRKaE1TMDRZekE0TFRobFkyWmtPRFpsTnpOa1lTSjkKMDA2NWNpZCBzeXN0ZW1fZGF0YT1leUp2Y21kaGJtbDZZWFJwYjI1SlJDSTZJQ0psTldOaE1EQmhOQzA1TWpGakxUUTBZMkV0WVRGa1l5MDRPVE01WkRnMllXSXhORElpZlE9PQowMDJmc2lnbmF0dXJlIKqGYQCp1rzCOhceuAY/0CU1pVdcYQZbG79FfEwT66CNCg==",
          :expires_at=>"2022-08-26T19:20:33.4611672Z"})

        manager = PublicKeyManager.new(imp_id: @imp_id, org_id: @org_id)

        new_system = manager.create_system(
          org_name: 'Org Name',
          public_key: file_fixture('stubbed_key.pem').read,
          signature: file_fixture('stubbed_signature.pem').read
        )

        expect(new_system[:response]).to eq(true)
        expect(new_system[:message]).to have_key(:client_token)
      end
    end

    context 'with invalid key' do
      it 'returns false when key is private' do
        manager = PublicKeyManager.new(imp_id: @imp_id, org_id: @org_id)
        new_system = manager.create_system(
          org_name: 'Org Name',
          public_key: file_fixture('private_key.pem').read,
          signature: file_fixture('stubbed_signature.pem').read
        )

        expect(new_system[:response]).to eq(false)
        expect(new_system[:message]).to eq("Must have valid encoding")
      end
    end
  end
end