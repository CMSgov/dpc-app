# frozen_string_literal: true

require 'rails_helper'

SESSION_CREATED_AT = 1.hour.ago.to_i
SESSION_COMPLETED_AT = Time.now.to_i

RSpec.describe CspVerification do
  let(:service) { described_class.new }
  let(:verification_id) { 'verify_abc123' }
  let(:url) { "#{CLEAR_CONFIG[:verification_session_endpoint]}/#{verification_id}" }
  let(:token) { JWT.encode({ 'verification_id' => verification_id }, nil, 'none') }

  describe '#supported?' do
    it 'is true for CSPs that have an adapter' do
      expect(service.supported?('clear')).to be true
      expect(service.supported?(:clear)).to be true
    end

    it 'is false for CSPs without one' do
      %w[id_me login_dot_gov].each { |csp| expect(service.supported?(csp)).to be false }
      expect(service.supported?(nil)).to be false
    end
  end

  describe '#verify' do
    # #verify doesn't raise: when CLEAR can't be asked, callers get an :unavailable result and fail closed
    shared_examples 'unavailable' do |reason|
      it "is unavailable because of #{reason}" do
        expect(verify).to eq(outcome: :unavailable, failure_reason: reason)
        expect(described_class::FAILURE_REASONS).to include(reason)
      end
    end

    shared_examples 'unavailable without asking CLEAR' do |reason|
      it_behaves_like 'unavailable', reason

      it 'makes no request' do
        verify
        expect(a_request(:any, /.*/)).not_to have_been_made
      end
    end

    let(:body) { { status: 'success' } }

    def verify = service.verify('clear', token)

    before do
      stub_request(:get, url).with(headers: { 'Authorization' => "Bearer #{token}" })
                             .to_return(status: 200, body: body.to_json)
    end

    it 'requests the session named in the access token, without revealing sensitive data' do
      verify
      expect(a_request(:get, url)).to have_been_made.once
    end

    it 'refuses a CSP without an adapter' do
      expect { service.verify('id_me', 'token') }.to raise_error(ArgumentError, /No verification adapter/)
    end

    context 'when CLEAR says the session passed' do
      it 'is verified' do
        expect(verify).to include(outcome: :verified, status: 'success')
      end
    end

    context 'when CLEAR says the session failed' do
      let(:body) do
        { id: verification_id, status: 'fail', status_details: 'ip_blocked',
          created_at: SESSION_CREATED_AT, completed_at: SESSION_COMPLETED_AT,
          checks: [{ name: 'Gov ID is not expired', value: false, status: 'completed', check_result: 'fail',
                     reason_codes: %w[doc_expired],
                     additional_details: { watchlist_hits: { hits: [{ details: { name: ['SECRET NAME'] } }] } } }],
          traits: { ssn9: '123456789' } }
      end

      it 'returns the outcome, the status and reason CLEAR gave, and what we store about the session' do
        expect(verify).to eq(
          outcome: :failed, status: 'fail', reason: 'ip_blocked', session_id: verification_id,
          created_at: Time.zone.at(SESSION_CREATED_AT), completed_at: Time.zone.at(SESSION_COMPLETED_AT),
          checks: [{ name: 'Gov ID is not expired', status: 'completed', status_details: 'fail', value: false,
                     reason_codes: %w[doc_expired] }]
        )
      end
    end

    describe 'when CLEAR cannot be asked' do
      {
        401 => 'unauthorized', 403 => 'forbidden', 404 => 'not_found', 429 => 'rate_limited', 500 => 'server_error'
      }.each do |code, reason|
        context "and responds with a #{code}" do
          before { stub_request(:get, url).to_return(status: code) }

          it_behaves_like 'unavailable', reason
        end
      end

      context 'and the request times out' do
        before { stub_request(:get, url).to_timeout }

        it_behaves_like 'unavailable', 'request_failed'
      end

      context 'and the connection is refused' do
        before { stub_request(:get, url).to_raise(Errno::ECONNREFUSED) }

        it_behaves_like 'unavailable', 'request_failed'
      end

      context 'and the response is not JSON' do
        before { stub_request(:get, url).to_return(status: 200, body: 'nope') }

        it_behaves_like 'unavailable', 'invalid_response'
      end

      it 'logs why' do
        stub_request(:get, url).to_return(status: 401)
        allow(Rails.logger).to receive(:error)
        verify
        expect(Rails.logger).to have_received(:error).with('CSP verification unavailable (csp=clear): unauthorized')
      end
    end

    describe 'when the access token is no good' do
      context 'because it is missing' do
        let(:token) { nil }

        it_behaves_like 'unavailable without asking CLEAR', 'missing_access_token'
      end

      context 'because it is not a JWT' do
        let(:token) { 'bearer-token' }

        it_behaves_like 'unavailable without asking CLEAR', 'invalid_access_token'
      end

      context 'because it has no verification_id' do
        let(:token) { JWT.encode({ 'sub' => 'x' }, nil, 'none') }

        it_behaves_like 'unavailable without asking CLEAR', 'invalid_verification_id'
      end

      context 'because its verification_id has unexpected characters' do
        let(:token) { JWT.encode({ 'verification_id' => '../../v1/users' }, nil, 'none') }

        it_behaves_like 'unavailable without asking CLEAR', 'invalid_verification_id'
      end
    end
  end
end

RSpec.describe CspVerification::Clear do
  describe '.session_url' do
    it 'is the session named in the access token' do
      token = JWT.encode({ 'verification_id' => 'verify_abc123' }, nil, 'none')
      expect(described_class.session_url(token))
        .to eq "#{CLEAR_CONFIG[:verification_session_endpoint]}/verify_abc123"
    end

    shared_examples 'a rejected access token' do |reason|
      it "raises #{reason}" do
        expect { described_class.session_url(token) }.to raise_error(CspVerification::Error, reason)
      end
    end

    context 'when the token is missing' do
      let(:token) { nil }

      it_behaves_like 'a rejected access token', 'missing_access_token'
    end

    context 'when the token is not a JWT' do
      let(:token) { 'bearer-token' }

      it_behaves_like 'a rejected access token', 'invalid_access_token'
    end

    context 'when the token has no verification_id' do
      let(:token) { JWT.encode({ 'sub' => 'x' }, nil, 'none') }

      it_behaves_like 'a rejected access token', 'invalid_verification_id'
    end

    context 'when the verification_id has unexpected characters' do
      let(:token) { JWT.encode({ 'verification_id' => '../../v1/users' }, nil, 'none') }

      it_behaves_like 'a rejected access token', 'invalid_verification_id'
    end
  end

  describe '.status_from' do
    it 'is the status CLEAR sent' do
      expect(described_class.status_from('status' => 'awaiting_manual_review')).to eq 'awaiting_manual_review'
    end
  end

  describe '.reason_from' do
    it 'is status_details' do
      expect(described_class.reason_from('status_details' => 'blocked_by_provider')).to eq 'blocked_by_provider'
    end

    it 'is status_reason.code when there is no status_details' do
      expect(described_class.reason_from('status_reason' => { 'code' => 'underage_user' })).to eq 'underage_user'
    end

    it 'is nil when CLEAR gives none' do
      expect(described_class.reason_from('status' => 'canceled')).to be_nil
    end
  end

  describe '.outcome_for' do
    {
      ['success', nil] => :verified,
      ['canceled', nil] => :cancelled,
      %w[canceled authentication_error] => :failed,
      ['fail', nil] => :failed,
      ['awaiting_manual_review', nil] => :unavailable
    }.each do |(status, reason), outcome|
      it "is #{outcome} for #{status}#{" with #{reason}" if reason}" do
        expect(described_class.outcome_for(status, reason)).to eq outcome
      end
    end

    it 'is unavailable for a status it does not know, rather than assumed to be a pass' do
      expect(described_class.outcome_for('something_new', nil)).to eq :unavailable
    end
  end

  describe '.details_from' do
    let(:body) do
      { 'id' => 'verify_abc123', 'created_at' => SESSION_CREATED_AT, 'completed_at' => SESSION_COMPLETED_AT,
        'checks' => [{ 'name' => 'Gov ID is not expired', 'value' => false, 'status' => 'completed',
                       'check_result' => 'fail', 'reason_codes' => %w[doc_expired],
                       'additional_details' => { 'watchlist_hits' => 'SECRET NAME' } },
                     { 'name' => 'Selfie passes liveness check', 'value' => true, 'status' => 'completed' }],
        'traits' => { 'ssn9' => '123456789' } }
    end

    it 'is the session id, its times (epoch seconds), and its checks' do
      expect(described_class.details_from(body)).to eq(
        session_id: 'verify_abc123',
        created_at: Time.zone.at(SESSION_CREATED_AT),
        completed_at: Time.zone.at(SESSION_COMPLETED_AT),
        checks: [{ name: 'Gov ID is not expired', status: 'completed', status_details: 'fail', value: false,
                   reason_codes: %w[doc_expired] },
                 { name: 'Selfie passes liveness check', status: 'completed', status_details: nil, value: true,
                   reason_codes: nil }]
      )
    end

    it 'has nil times and no checks for a session that has not started or finished' do
      expect(described_class.details_from('id' => 'verify_abc123', 'created_at' => 0, 'completed_at' => 0))
        .to eq(session_id: 'verify_abc123', created_at: nil, completed_at: nil, checks: [])
    end
  end
end
