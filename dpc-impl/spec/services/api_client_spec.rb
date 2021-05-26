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
          {
            'id': '923a4f7b-eade-494a-8ca4-7a685edacfad',
            'name': 'Wayfarer',
            'created_at': 00000,
            'updated_at': 00000
          }
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

end