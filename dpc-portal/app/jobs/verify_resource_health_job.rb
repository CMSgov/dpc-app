# frozen_string_literal: true

# A background job that verifies that external services are up and accessible.
class VerifyResourceHealthJob < ApplicationJob
  queue_as :portal

  def perform
    client = DpcClient.new
    client.get_healthcheck
    puts client.response_successful?
  end
end
