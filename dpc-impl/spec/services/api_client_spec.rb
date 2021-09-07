# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApiClient do

  describe '#create_implementer' do
    context 'successful API request' do
      it 'sends data to API and sets response instance variables' do
        stub_request(:post, "http://dpc.example.com/Implementer").
        with(
          body: "{\"name\":\"Wayfarer\"}"
        ).to_return(
          status: 200, 
          body: "{'id': '923a4f7b-eade-494a-8ca4-7a685edacfad'," \
                "'name': 'Wayfarer'," \
                "'created_at': 00000," \
                "'updated_at': 00000}"
        )

        api_client = ApiClient.new

        api_client.create_implementer('Wayfarer')

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          "{'id': '923a4f7b-eade-494a-8ca4-7a685edacfad','name': 'Wayfarer','created_at': 00000,'updated_at': 00000}"
        )
      end
    end

    context 'unsuccessful API request' do
      it 'responds like 500 if connection error is raised' do
        http_stub = instance_double(Net::HTTP)
        allow(Net::HTTP).to receive(:new).and_return(http_stub)
        allow(http_stub).to receive(:request).and_raise(Errno::ECONNREFUSED)

        api_client = ApiClient.new

        api_client.create_implementer('Wayfarer')

        expect(api_client.response_status).to eq(500)
        expect(api_client.response_body).to eq(
          {
            'issue' => [{
              'details' => {
                'text' => 'Connection error'
              }
            }]
          }
        )
      end
    end
  end

  describe '#get_provider_orgs' do
    context 'successful API request' do
      it 'responds with provider_orgs array' do
        user = create(:user)
        imp_id = user.implementer_id

        provider_orgs = [{:org_id=>"040b5bca-9d88-4891-8bda-6ef8a71c4b8b",
                        :org_name=>"Shiny Xenon Healthcare",
                        :status=>"Active",
                        :npi=>"4141690865"}]

        api_client = instance_double(ApiClient)
        allow(ApiClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:get_provider_orgs)
          .with(imp_id)
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return(provider_orgs)

        api_request = ApiClient.new
        get_request = api_request.get_provider_orgs(imp_id)

        expect(get_request.response_body).to eq(provider_orgs)
      end
    end

    context 'failed API request' do
      it 'responds with empty array' do
        user = create(:user)
        imp_id = user.implementer_id

        api_client = instance_double(ApiClient)
        allow(ApiClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:get_provider_orgs)
          .with(imp_id)
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(false)
        allow(api_client).to receive(:response_body).and_return(error: 'Bad request')

        api_request = ApiClient.new

        expect(user.provider_orgs).to eq(false)
      end
    end
  end

  describe '#create_provider_org' do
    context 'successful API request' do
      it 'returns 200 with org info' do
        imp_id = 'dbc4de2c-988d-4ef2-a264-db07fd7672c6'
        npi = '5350364407'
        uri_string = "http://dpc.example.com/Implementer/" + imp_id + "/org"

        stub_request(:post, uri_string).
          with(
            body: "{\"npi\":\"5350364407\"}"
          ).to_return(
            status: 200, 
            body: "{'id': 'dbc4de2c-988d-4ef2-a264-db07fd7672c6',"\
                  "'org_id': '72b8ffe4-8966-4a15-86c8-1faae680057d',"\
                  "'implementer_id': '3008bd84-34dc-42a1-8c08-8ecfd86e73da',"\
                  "'ssas_system_id': '4',"\
                  "'status': 'Active',"\
                  "'npi': '5350364407'}"
          )

        api_client = ApiClient.new

        api_client.create_provider_org(imp_id,npi)

        expect(api_client.response_status).to eq(200)
        expect(api_client.response_body).to eq(
          "{'id': 'dbc4de2c-988d-4ef2-a264-db07fd7672c6','org_id': '72b8ffe4-8966-4a15-86c8-1faae680057d','implementer_id': '3008bd84-34dc-42a1-8c08-8ecfd86e73da','ssas_system_id': '4','status': 'Active','npi': '5350364407'}"
        )
      end
    end
  end
end