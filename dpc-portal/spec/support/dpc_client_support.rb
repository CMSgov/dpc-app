# frozen_string_literal: true

module DpcClientSupport
  def stub_api_client(message:, response: {})
    doubled_client = instance_double(DpcClient)
    allow(DpcClient).to receive(:new).and_return(doubled_client)
    allow(doubled_client).to receive(message).and_return(response)
    doubled_client
  end

  # rubocop:disable Metrics/AbcSize
  def stub_self_returning_api_client(message:, success: true, response: {}, api_client: nil, with: nil)
    doubled_client = api_client || instance_double(DpcClient)
    allow(DpcClient).to receive(:new).and_return(doubled_client)
    if with
      allow(doubled_client).to receive(message)
        .with(*with)
        .and_return(doubled_client)
    else
      allow(doubled_client).to receive(message).and_return(doubled_client)
    end
    allow(doubled_client).to receive(:response_successful?).and_return(success)
    allow(doubled_client).to receive(:response_body).and_return(response)
    doubled_client
  end
  # rubocop:enable Metrics/AbcSize

  def default_get_org_response(api_id)
    FHIR::Organization.new(
      name: "Bob's Health Hut",
      id: api_id
    )
  end

  def default_get_public_keys
    { 'entities' =>
      [{ 'id' => '579dd199-3c2d-48e8-8594-cec35e223527',
         'publicKey' =>
         "-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAyLrLpbFduLeGG7Eh2KdD
koPuyOLZ4oalcqVMGJFeYpcCAwEAAQ==
-----END PUBLIC KEY-----",
         'createdAt' => '2020-09-10T02:30:27.526+00:00',
         'label' => 'aws-scripts' }] }
  end

  def default_get_client_tokens
    { 'entities' =>
      [{ 'id' => 'bd49166a-f896-400f-aaa2-c6fa953e1128',
         'tokenType' => 'MACAROON',
         'label' => 'Token for organization 4b15098b-d53f-432d-a2f3-416a9483527b.',
         'createdAt' => '2020-09-10T02:45:07.452+00:00',
         'expiresAt' => '2021-09-10T02:45:07.449+00:00' }] }
  end
end
