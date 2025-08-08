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

  def stub_api_call(message:, response: {}, api_client: nil)
    doubled_client = api_client || instance_double(DpcClient)
    allow(DpcClient).to receive(:new).and_return(doubled_client)
    allow(doubled_client).to receive(message).and_return(response)
    doubled_client
  end

  def default_org_creation_response
    {
      'id' => '923a4f7b-eade-494a-8ca4-7a685edacfad'
    }
  end
end

class MockFHIRResponse
  attr_reader :entry

  def initialize(entries_count: 0)
    @entry = Array.new(entries_count) { MockEntry.new }
  end
end

class MockEntry
  attr_reader :resource

  def initialize
    @resource = MockResource.new
  end
end

class MockResource
  attr_reader :id

  def initialize
    @id = rand(0..9)
  end
end

class MockOrgResponse
  attr_reader :response_body

  def initialize(response_successful: true, response_body: {})
    @response_successful = response_successful
    @response_body = response_body
  end

  def response_successful?
    @response_successful
  end
end
