# frozen_string_literal: true

class LogOrganizationsAccessJob < ApplicationJob
  queue_as :portal

  def perform
    @start = Time.now
    fetch_organizations.each do |organization|
      credential_status = fetch_credential_status(organization)
      Rails.logger.info("Organization: #{organization['name']}, Credential Status: #{credential_status}")
    rescue Exception => e
      Rails.logger("failed to retrieve credential status for organization: #{organization}")
    end
  end

  def fetch_organizations
    url = URI("https://#{put_env_here}.dpc.cms.gov/#{put_endpoint_here}")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Get.new(url)
    request["Content-Type"] = "application/x-www-form-urlencoded"
    request["Accept"] = "application/json"
    request.body = "put_details_here"
    response = https.request(request)

    return JSON.parse(response)["organizations"]
  rescue StandardError => e
    Rails.logger.error("Failed to fetch companies: #{e.message}")
    raise
  end

  def fetch_credential_status(organization)
      nil
  end
end
