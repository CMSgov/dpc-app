# frozen_string_literal: true

require 'aws-sdk-cloudwatch'

# A background job that verifies that external services are up and accessible.
class VerifyResourceHealthJob < ApplicationJob
  queue_as :portal

  METRIC_NAMESPACE = 'DPC'
  REGION = 'us-east-1'
  ENVIRONMENT = ENV.fetch('ENV', 'none')

  # Runs all healthchecks if no args provided
  def perform(check_dpc: true, check_idp: true, check_cpi: true)
    dpc_healthcheck if check_dpc
    idp_healthcheck if check_idp
    cpi_gateway_healthcheck if check_cpi
  end

  private

  def dpc_healthcheck
    dpc_client = DpcClient.new
    dpc_client.healthcheck
    unless dpc_client.response_successful?
      Rails.logger.warn([dpc_client.response_body.to_s,
                         { actionContext: LoggingConstants::ActionContext::HealthCheck }])
    end

    log_healthcheck(
      'PortalConnectedToDpcApi',
      dpc_client.response_successful?
    )
  end

  def idp_healthcheck
    CspConfig.all.each do |csp|
      csp_host = csp.host
      csp_name = csp.code
      oidc_discovery_url = csp.discovery_uri
      if csp_host.nil? || oidc_discovery_url.nil?
        log_healthcheck('PortalConnectedToCsp', false, csp_host:, csp_name:)
      else
        # None of our CSPs have a healthcheck, so we'll try the OIDC well-known endpoint
        response = Net::HTTP.get_response(URI("https://#{csp_host}#{oidc_discovery_url}"))
        log_healthcheck(
          'PortalConnectedToCsp',
          response.code.to_i.between?(200, 299),
          csp_host:,
          csp_name:
        )
      end
    end
  end

  def cpi_gateway_healthcheck
    cpi_client = CpiApiGatewayClient.new
    auth_health = cpi_client.healthy_auth?
    api_health = cpi_client.healthy_api?

    unless auth_health
      Rails.logger.warn(['CPI API gateway auth endpoint is currently down',
                         { actionContext: LoggingConstants::ActionContext::HealthCheck }])
    end
    unless api_health
      Rails.logger.warn(['CPI API gateway api endpoints are currently down',
                         { actionContext: LoggingConstants::ActionContext::HealthCheck }])
    end

    log_healthcheck(
      'PortalConnectedToCpiApiGateway',
      auth_health && api_health
    )
  end

  def log_healthcheck(check_name, healthy, csp_host: nil, csp_name: nil)
    action_type = if healthy
                    LoggingConstants::ActionType::HealthCheckPassed
                  else
                    LoggingConstants::ActionType::HealthCheckFailed
                  end
    Rails.logger.info(["Healthcheck #{check_name}", { actionContext: LoggingConstants::ActionContext::HealthCheck,
                                                      actionType: action_type, csp_host:, csp_name: }])
    emit_cloudwatch_metric(check_name, healthy, idp: csp_name)
  end

  def dimensions(idp = nil)
    dims = []
    dims << { name: 'Type', value: 'healthcheck' }
    dims << { name: 'environment', value: ENVIRONMENT }
    dims << { name: 'idp', value: idp } if idp
    dims
  end

  def emit_cloudwatch_metric(check_name, healthy, idp: nil)
    Aws::CloudWatch::Client.new(region: REGION).put_metric_data(
      {
        namespace: METRIC_NAMESPACE,
        metric_data: [
          {
            metric_name: check_name,
            dimensions: dimensions(idp),
            value: healthy ? 1 : 0,
            unit: 'None'
          }
        ]
      }
    )
  rescue StandardError
    # If we're not running on AWS, or don't have the AWS CLI configured, we'll get an error.
    # This is normal when running locally, so only logging in debug mode.
    Rails.logger.debug(["Could not emit metric #{check_name} to AWS",
                        { actionContext: LoggingConstants::ActionContext::HealthCheck }])
  end
end
