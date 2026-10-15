# frozen_string_literal: true

require 'json'
require 'fileutils'

class CaptureCpiGatewayResponsesJob < ApplicationJob
  queue_as :portal

  REQUESTS = [
    ['AO_WITH_MED_SANCTIONS', :fetch_profile, '1962588129'],
    ['AO_WITH_WAIVERS', :fetch_med_sanctions_and_waivers_by_ssn, '377588611'],
    ['ORG_WITH_AO_SSN', :fetch_profile, '1174520977']
  ].freeze

  def perform
    client = build_client
    directory = File.expand_path('./cpi_gateway_response')
    FileUtils.mkdir_p(directory, mode: 0o700)
    Rails.logger.info "Saving CPI gateway responses to #{directory}"

    failed_requests = REQUESTS.filter_map do |label, method, identifier|
      response, failed = fetch_response(client, label, method, identifier)
      save_response(directory, label, response)
      label if failed
    end

    unless failed_requests.empty?
      raise "CPI gateway requests failed: #{failed_requests.join(', ')}; responses saved in #{directory}"
    end

    directory
  end

  private

  def build_client
    configuration = JSON.parse(ENV.fetch('CPI_API_GW_TESTDATA')).fetch('meta').fetch('configuration').fetch('test')
    CpiApiGatewayClient.new(
      cms_idm_url: configuration.fetch('OAUTH_URL'),
      cpi_api_gateway_url: configuration.fetch('BASE_URL')
    )
  end

  def fetch_response(client, label, method, identifier)
    [client.public_send(method, identifier), false]
  rescue OAuth2::Error => e
    Rails.logger.warn "#{label}: #{e.class}; saving the HTTP error response"
    [{ error: e.class.name, status: e.response.status, response_body: e.response.body }, true]
  rescue StandardError => e
    Rails.logger.warn "#{label}: #{e.class}; saving error details"
    [{ error: e.class.name, message: e.message }, true]
  end

  def save_response(directory, label, response)
    path = File.join(directory, "#{label}.json")
    File.open(path, File::WRONLY | File::CREAT | File::TRUNC, 0o600) do |file|
      file.puts JSON.pretty_generate(response)
    end
    Rails.logger.info "#{label}: saved #{path}"
  end
end
