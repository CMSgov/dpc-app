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
      'created_at' => DateTime.now,
      'updated_at' => DateTime.now
    }
  end

  def default_add_provider_org_response
    {'status':"Active",
     'id':"2e449356-903f-4dbe-9a32-9a45d08c26a9",
     'implementer_id':"923a4f7b-eade-494a-8ca4-7a685edacfad",
     'organization_id':"587ed9cf-6bd6-4860-9de3-077277b2c824",
     'created_at':"2021-07-06T17:46:59.365253Z",
     'updated_at':"2021-07-06T17:46:59.365253Z"}
  end

  def default_get_provider_org_response
    {'id' =>'58bb33bd-8c5a-4ff9-8113-0a8996b9c11e',
     'identifier' => [{'system' => 'http://hl7.org/fhir/sid/us-npi', 'value' => '3103929527'}],
     'meta' =>
      {'id' => 'Organization/58bb33bd-8c5a-4ff9-8113-0a8996b9c11e',
       'lastUpdated' => '2021-08-02T19:01:28.878+00:00',
       'versionId' => '0'},
     'name' => 'Purple Elephant Healthcare',
     'resourceType' =>'Organization'}
  end

  def default_provider_orgs_list
    [
      {"org_id": "58bb33bd-8c5a-4ff9-8113-0a8996b9c11e",
       "org_name": "Purple Elephant Healthcare",
       "status": "Active",
       "npi": "3103929527",
       "ssas_system_id": ""},
      {"org_id": "28f3a00d-d714-44df-8844-7ec792adcd88",
       "org_name": "Festive Kaftan Healthcare",
       "status": "Active",
       "npi": "3092016294",
       "ssas_system_id": ""},
      {"org_id": "6c9a5685-7cbe-42f1-9668-f139e67654ea",
       "org_name": "Gregarious Capitol Healthcare",
       "status": "Active",
       "npi": "3718986623",
       "ssas_system_id": ""}
    ]
  end
end
