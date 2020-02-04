# frozen_string_literal: true

module APIClientSupport
  def stub_api_client(api_client: nil, message:, success: true, response: {})
    doubled_client = api_client || instance_double(APIClient)
    allow(APIClient).to receive(:new).and_return(doubled_client)
    allow(doubled_client).to receive(message).and_return(doubled_client)
    allow(doubled_client).to receive(:response_body).and_return(response)
    allow(doubled_client).to receive(:response_successful?).and_return(success)
    doubled_client
  end

  def default_org_creation_response
    {
      'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad',
      'endpoint' => [
        'reference' => 'Endpoint/437f7b17-3d48-4654-949d-57ea80f8f1d7'
      ]
    }
  end
end
