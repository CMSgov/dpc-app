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

  def default_provider_org_response
    {:status=>"Active",
     :id=>"2e449356-903f-4dbe-9a32-9a45d08c26a9",
     :implementer_id=>"923a4f7b-eade-494a-8ca4-7a685edacfad",
     :organization_id=>"587ed9cf-6bd6-4860-9de3-077277b2c824",
     :created_at=>"2021-07-06T17:46:59.365253Z",
     :updated_at=>"2021-07-06T17:46:59.365253Z"}
  end
end
