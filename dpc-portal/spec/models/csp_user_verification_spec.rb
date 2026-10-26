# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspUserVerification, type: :model do
  let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }
  let(:session_created_at) { Time.zone.at(1_761_677_000) }
  let(:completed_at) { Time.zone.at(1_761_677_082) }
  let(:checks) do
    [{ name: 'Gov ID is not expired', status: 'completed', status_details: 'pass', value: true,
       reason_codes: [], completed_at: nil },
     { name: 'Selfie passes liveness check', status: 'completed', status_details: nil, value: false,
       reason_codes: %w[liveness_failed], completed_at: completed_at }]
  end
  let(:failure) do
    { status: 'fail', outcome: :failed, reason: 'authentication_error', session_id: 'verify_abc123',
      session_created_at:, completed_at:, checks: }
  end

  describe '.record!' do
    it 'stores a failure with its details and checks' do
      record = described_class.record!(csp:, verification: failure)

      expect(record).to be_persisted
      expect(record).to have_attributes(csp:, csp_verification_id: 'verify_abc123', status: 'fail',
                                        session_created_at:, completed_at:)
      expect(record.csp_verification_checks.order(:id).map { |c| [c.check_name, c.status_value, c.reason_codes] })
        .to eq [['Gov ID is not expired', true, []], ['Selfie passes liveness check', false, %w[liveness_failed]]]
    end

    it "stores the CSP's creation time apart from when we recorded the session" do
      record = described_class.record!(csp:, verification: failure)
      expect(record.session_created_at).to eq session_created_at
      expect(record.created_at).to be_within(1.minute).of(Time.current)
    end

    it 'leaves session_created_at empty when the CSP gave none' do
      record = described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_other' })
      expect(record.session_created_at).to be_nil
    end

    it 'links an existing user to the user' do
      user = create(:user)
      invitation = create(:invitation, :ao)
      record = described_class.record!(csp:, verification: failure, user:, invitation:)
      expect(record.user).to eq user
      expect(record.invitation).to be_nil
    end

    it 'links a new user to the invitation' do
      invitation = create(:invitation, :ao)
      record = described_class.record!(csp:, verification: failure, invitation:)
      expect(record.user).to be_nil
      expect(record.invitation).to eq invitation
    end

    it 'allows a session for someone we cannot identify' do
      record = described_class.record!(csp:, verification: failure)
      expect([record.user, record.invitation]).to eq [nil, nil]
    end

    it 'keeps each response as its own row, with its own checks' do
      first = described_class.record!(csp:, verification: failure)
      later = described_class.record!(csp:, verification: failure.merge(status: 'manual_success'))

      expect(described_class.where(csp_verification_id: 'verify_abc123').count).to eq 2
      expect(first.reload.status).to eq 'fail'
      expect(later.status).to eq 'manual_success'
      expect([first, later].map { |row| row.csp_verification_checks.count }).to eq [checks.length, checks.length]
    end

    it 'puts the latest response for a session last by created_at' do
      first = described_class.record!(csp:, verification: failure)
      later = described_class.record!(csp:, verification: failure.merge(status: 'manual_success'))

      expect(described_class.where(csp_verification_id: 'verify_abc123').order(:created_at, :id)).to eq [first, later]
    end

    it 'records a response with no checks' do
      record = described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_abc123' })
      expect(record.csp_verification_checks).to be_empty
    end

    it 'stores nothing when one of the checks is invalid' do
      bad = failure.merge(checks: [{ name: 'Gov ID is not expired', status: 'completed' }, { name: nil }])

      expect { described_class.record!(csp:, verification: bad) }.to raise_error(ActiveRecord::RecordInvalid)
      expect(described_class.count).to eq 0
      expect(CspVerificationCheck.count).to eq 0
    end

    it 'requires a session id' do
      expect { described_class.record!(csp:, verification: { status: 'fail' }) }
        .to raise_error(ActiveRecord::RecordInvalid)
    end
  end

  it 'requires a session id and status' do
    expect(described_class.new(csp:)).not_to be_valid
  end
end
