# frozen_string_literal: true

module DpcClientSupport
  def stub_api_client(message:, success: true, response: {}, api_client: nil)
    doubled_client = api_client || instance_double(DpcClient)
    allow(DpcClient).to receive(:new).and_return(doubled_client)
    allow(doubled_client).to receive(message).and_return(doubled_client)
    allow(doubled_client).to receive(:response_body).and_return(response)
    allow(doubled_client).to receive(:response_successful?).and_return(success)
    doubled_client
  end

  def stub_multiple_call_client(messages:, responses:)
    doubled_client = instance_double(DpcClient)
    allow(DpcClient).to receive(:new).and_return(doubled_client)
    messages.each do |message|
      allow(doubled_client).to receive(message).and_return(doubled_client)
    end
    allow(doubled_client).to receive(:response_successful?).and_return(true)
    allow(doubled_client).to receive(:response_body).and_return(*responses)
    doubled_client
  end

  def default_org_creation_response
    {
      'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad'
    }
  end
end
