# frozen_string_literal: true

require 'aws-sdk-cloudwatch'

# A background job that verifies that external services are up and accessible.
class VerifyResourceHealthJob < ApplicationJob
  queue_as :portal

  METRIC_NAMESPACE = 'DPC'
  REGION = 'us-east-1'
  ENVIRONMENT = ENV.fetch('ENV', 'none')

  def perform
    client = DpcClient.new
    client.get_healthcheck
    logger.warn(client.response_body) unless client.response_successful?

    log_healthcheck(
      'PortalConnectedToDpcApi',
      client.response_successful?
    )

    nil
  end

  private

  def log_healthcheck(
    check_name,
    healthy
  )
    action_type = if healthy
                    LoggingConstants::ActionType::HealthCheckPassed
                  else
                    LoggingConstants::ActionType::HealthCheckFailed
                  end
    logger.info(["Healthcheck #{check_name}",
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
        value: environment
      }
    ]
  end

  def emit_cloudwatch_metric(
    check_name,
    healthy
  )
    Aws::CloudWatch::Client.new(region: REGION).put_metric_data(
      namespace: METRIC_NAMESPACE,
      metric_data: [
        {
          metric_name: check_name,
          dimensions:,
          value: healthy ? 1 : 0,
          unit: 'None'
        }
      ]
    )
  rescue StandardError
    # If we're not running on AWS, or don't have the AWS CLI configured, we'll get an error.
    # This is normal when running locally, so only logging in debug mode.
    logger.debug(["Could not emit metric #{check_name} to AWS",
                  { actionContext: LoggingConstants::ActionContext::HealthCheck }])
  end
end
