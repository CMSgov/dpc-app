# frozen_string_literal: true

module ApiClientSupport
  def stub_api_client(api_client: nil, message:, success: true, response: {})
    doubled_client = api_client || instance_double(ApiClient)
    allow(ApiClient).to receive(:new).and_return(doubled_client)
    allow(doubled_client).to receive(message).and_return(doubled_client)
    allow(doubled_client).to receive(:response_body).and_return(response)
    allow(doubled_client).to receive(:response_successful?).and_return(success)
    doubled_client
  end

  def default_imp_creation_response
    {
      'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad',
      'name' => 'Surreal Kayak',
      'created_at' => 00000,
      'updated_at' => 00000
    }
  end
end
