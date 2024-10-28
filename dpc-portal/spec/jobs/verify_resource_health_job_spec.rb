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

  let(:job) { VerifyResourceHealthJob.new }

  context 'can successfully send metrics' do
    describe 'everything healthy' do
      it 'should emit healthy metrics' do
        expect_dpc_api
        expect_cpi
        expect_idp

        job.perform
      end
    end

    describe 'dpc-api is down' do
      it 'should emit an unhealthy metric' do
        expect_dpc_api(response_successful: false, metric: 0)
        expect_cpi
        expect_idp

        job.perform
      end
    end

    describe 'cpi gateway auth is down' do
      it 'should emit an unhealthy metric' do
        expect_dpc_api
        expect_cpi(auth_health: false, metric: 0)
        expect_idp

        job.perform
      end
    end

    describe 'cpi gateway api is down' do
      it 'should emit an unhealthy metric' do
        expect_dpc_api
        expect_cpi(api_health: false, metric: 0)
        expect_idp

        job.perform
      end
    end

    describe 'idp is down' do
      it 'should emit an unhealthy metric' do
        expect_dpc_api
        expect_cpi
        expect_idp(site_status: 500, metric: 0)

        job.perform
      end
    end

    describe 'idp is not configured' do
      let!(:previous_idp) { VerifyResourceHealthJob::IDP_HOST }
      before do
        VerifyResourceHealthJob::IDP_HOST = nil
      end
      after do
        VerifyResourceHealthJob::IDP_HOST = previous_idp
      end

      it 'should emit an unhealthy metric when url is not configured' do
        expect_dpc_api
        expect_cpi
        expect_idp(metric: 0)

        job.perform
      end
    end
  end

  context 'not connected to AWS' do
    it 'should ignore connection error and move on gracefully' do
      stub_request(:get, 'https://idp.int.identitysandbox.gov').to_return(status: 200)

      expect(mock_dpc_client).to receive(:healthcheck)
      expect(mock_dpc_client).to receive(:response_successful?).and_return(true).twice
      expect(mock_cpi_client).to receive(:healthy_auth?).and_return(true)
      expect(mock_cpi_client).to receive(:healthy_api?).and_return(true)

      expect(mock_cloudwatch_client).to receive(:put_metric_data).and_raise(StandardError).thrice
      job.perform
    end
  end

  context 'only runs requested checks' do
    it 'runs dpc-api check' do
      expect_dpc_api
      job.perform({ 'check_dpc' => true, 'check_cpi' => false, 'check_idp' => false })
    end

    it 'runs idp and cpi checks' do
      expect_cpi
      expect_idp
      job.perform({ 'check_dpc' => false, 'check_cpi' => true, 'check_idp' => true })
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

  def expect_dpc_api(response_successful: true, metric: 1)
    expect(mock_dpc_client).to receive(:healthcheck)
    expect(mock_dpc_client).to receive(:response_successful?).twice.and_return(response_successful)
    allow(mock_dpc_client).to receive(:response_body).and_return({ 'healthcheck' => metric })
    expect_put_metric('PortalConnectedToDpcApi', metric)
  end

  def expect_cpi(auth_health: true, api_health: true, metric: 1)
    expect(mock_cpi_client).to receive(:healthy_auth?).and_return(auth_health)
    expect(mock_cpi_client).to receive(:healthy_api?).and_return(api_health)
    expect_put_metric('PortalConnectedToCpiApiGateway', metric)
  end

  def expect_idp(site_status: 200, metric: 1)
    stub_request(:get, 'https://idp.int.identitysandbox.gov').to_return(status: site_status)
    expect_put_metric('PortalConnectedToIdp', metric)
  end

  def expect_put_metric(name, value)
    expect(mock_cloudwatch_client).to receive(:put_metric_data).with(
      put_metric_data_parms(
        VerifyResourceHealthJob::METRIC_NAMESPACE,
        VerifyResourceHealthJob::ENVIRONMENT,
        name,
        value
      )
    )
  end
end
