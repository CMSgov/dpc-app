# frozen_string_literal: true

require 'aws-sdk-cloudwatch'

# A background job that verifies that external services are up and accessible.
class VerifyResourceHealthJob
  include Sidekiq::Job
  queue_as :portal

  METRIC_NAMESPACE = 'DPC'
  REGION = 'us-east-1'
  ENVIRONMENT = ENV.fetch('ENV', 'none')
  IDP_HOST = ENV.fetch('IDP_HOST', nil)

  # Runs all healthchecks if no args provided
  def perform(args = {})
    dpc_healthcheck if args.key?('check_dpc') ? args['check_dpc'] : true
    idp_healthcheck if args.key?('check_idp') ? args['check_idp'] : true
    cpi_gateway_healthcheck if args.key?('check_cpi') ? args['check_cpi'] : true
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
    return log_healthcheck('PortalConnectedToIdp', false) if IDP_HOST.nil?

    # Login.gov doesn't have a /healthcheck, so we look for a 200 to verify connectivity.
    response = Net::HTTP.get_response(URI("https://#{IDP_HOST}"))
    log_healthcheck(
      'PortalConnectedToIdp',
      response.code.to_i.between?(200, 299)
    )
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

  def log_healthcheck(check_name, healthy)
    action_type = if healthy
                    LoggingConstants::ActionType::HealthCheckPassed
                  else
                    LoggingConstants::ActionType::HealthCheckFailed
                  end
    Rails.logger.info(["Healthcheck #{check_name}",
                       { actionContext: LoggingConstants::ActionContext::HealthCheck,
                         actionType: action_type }])
    emit_cloudwatch_metric(check_name, healthy)
  end

  def dimensions
    [
      {
        name: 'Type',
        value: 'healthcheck'
      },
      {
        name: 'environment',
        value: ENVIRONMENT
      }
    ]
  end

  def emit_cloudwatch_metric(check_name, healthy)
    Aws::CloudWatch::Client.new(region: REGION).put_metric_data(
      {
        namespace: METRIC_NAMESPACE,
        metric_data: [
          {
            metric_name: check_name,
            dimensions:,
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
