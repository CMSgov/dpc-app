# frozen_string_literal: true

require 'rails_helper'

RSpec.describe VerifyResourceHealthJob, type: :job do
  let(:mock_dpc_client) { instance_double(DpcClient) }
  before do
    allow(DpcClient).to receive(:new).and_return(mock_dpc_client)
  end

  let(:mock_cpi_client) { instance_double(CpiApiGatewayClient) }
  before do
    allow(CpiApiGatewayClient).to receive(:new).and_return(mock_cpi_client)
  end

  let(:mock_cloudwatch_client) { instance_double(Aws::CloudWatch::Client) }
  before do
    allow(Aws::CloudWatch::Client).to receive(:new).with(region: 'us-east-1').and_return(mock_cloudwatch_client)
  end

  context 'can successfully send metrics' do
    describe 'everything healthy' do
      it 'should emit healthy metrics' do
        VerifyResourceHealthJob::IDP_HOST = 'www.idp_test.com'
        stub_request(:get, 'https://www.idp_test.com').to_return(status: 200)

        expect(mock_dpc_client).to receive(:get_healthcheck)
        expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(true)
        expect(mock_cpi_client).to receive(:healthcheck).and_return(true)
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToDpcApi',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToIdp',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToCpiApiGateway',
            1
          )
        )

        VerifyResourceHealthJob.perform_now
      end
    end

    describe 'dpc-api is down' do
      it 'should emit an unhealthy metric' do
        VerifyResourceHealthJob::IDP_HOST = 'www.idp_test.com'
        stub_request(:get, 'https://www.idp_test.com').to_return(status: 200)

        expect(mock_dpc_client).to receive(:get_healthcheck)
        expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(false)
        expect(mock_dpc_client).to receive(:response_body).and_return({ 'healthcheck' => false })
        expect(mock_cpi_client).to receive(:healthcheck).and_return(true)
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToDpcApi',
            0
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToIdp',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToCpiApiGateway',
            1
          )
        )

        VerifyResourceHealthJob.perform_now
      end
    end

    describe 'idp is down' do
      it 'should emit an unhealthy metric when url cannot be reached' do
        VerifyResourceHealthJob::IDP_HOST = 'www.idp_test.com'
        stub_request(:get, 'https://www.idp_test.com').to_return(status: 500)

        expect(mock_dpc_client).to receive(:get_healthcheck)
        expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(true)
        expect(mock_cpi_client).to receive(:healthcheck).and_return(true)
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToDpcApi',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToIdp',
            0
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToCpiApiGateway',
            1
          )
        )

        VerifyResourceHealthJob.perform_now
      end

      it 'should emit an unhealthy metric when url is not configured' do
        VerifyResourceHealthJob::IDP_HOST = nil
        stub_request(:get, 'https://www.idp_test.com').to_return(status: 200)

        expect(mock_dpc_client).to receive(:get_healthcheck)
        expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(true)
        expect(mock_cpi_client).to receive(:healthcheck).and_return(true)
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToDpcApi',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToIdp',
            0
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToCpiApiGateway',
            1
          )
        )

        VerifyResourceHealthJob.perform_now
      end
    end

    describe 'cpi gateway is down' do
      it 'should emit an unhealthy metric' do
        VerifyResourceHealthJob::IDP_HOST = 'www.idp_test.com'
        stub_request(:get, 'https://www.idp_test.com').to_return(status: 200)

        expect(mock_dpc_client).to receive(:get_healthcheck)
        expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(true)
        expect(mock_cpi_client).to receive(:healthcheck).and_return(false)
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToDpcApi',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToIdp',
            1
          )
        )
        expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
          put_metric_data_parms(
            VerifyResourceHealthJob::METRIC_NAMESPACE,
            VerifyResourceHealthJob::ENVIRONMENT,
            'PortalConnectedToCpiApiGateway',
            0
          )
        )

        VerifyResourceHealthJob.perform_now
      end
    end
  end

  context 'not connected to AWS' do
    it 'should ignore connection error and move on gracefully' do
      VerifyResourceHealthJob::IDP_HOST = 'www.idp_test.com'
      stub_request(:get, 'https://www.idp_test.com').to_return(status: 200)

      expect(mock_dpc_client).to receive(:get_healthcheck)
      expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(true)
      expect(mock_cpi_client).to receive(:healthcheck).and_return(true)

      allow(mock_cloudwatch_client).to receive(:put_metric_data).and_raise(StandardError)
      VerifyResourceHealthJob.perform_now
    end
  end

  private

  def put_metric_data_parms(namespace, env, check_name, value)
    {
      namespace:,
      metric_data: [
        {
          metric_name: check_name,
          dimensions: [
            {
              name: 'Type',
              value: 'healthcheck'
            },
            {
              name: 'environment',
              value: env
            }
          ],
          value:,
          unit: 'None'
        }
      ]
    }
  end
end
