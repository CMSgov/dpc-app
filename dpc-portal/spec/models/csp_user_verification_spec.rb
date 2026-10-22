# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspUserVerification, type: :model do
  let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }
  let(:created_at) { Time.zone.at(1_761_677_000) }
  let(:completed_at) { Time.zone.at(1_761_677_082) }
  let(:checks) do
    [{ name: 'Gov ID is not expired', status: 'completed', status_details: 'pass', value: true,
       reason_codes: [], completed_at: nil },
     { name: 'Selfie passes liveness check', status: 'completed', status_details: nil, value: false,
       reason_codes: %w[liveness_failed], completed_at: completed_at }]
  end
  let(:failure) do
    { status: 'fail', outcome: :failed, reason: 'authentication_error', session_id: 'verify_abc123',
      created_at:, completed_at:, checks: }
  end

  describe '.record!' do
    it 'stores a failure with its details and checks' do
      record = described_class.record!(csp:, verification: failure)

      expect(record).to be_persisted
      expect(record).to have_attributes(csp:, csp_verification_id: 'verify_abc123', status: 'fail', created_at:,
                                        completed_at:)
      expect(record.csp_verification_checks.order(:id).map { |c| [c.check_name, c.status_value, c.reason_codes] })
        .to eq [['Gov ID is not expired', true, []], ['Selfie passes liveness check', false, %w[liveness_failed]]]
    end

    it "uses the CSP's creation time as created_at, or the time we recorded it if the CSP gave none" do
      expect(described_class.record!(csp:, verification: failure).created_at).to eq created_at

      no_time = described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_other' })
      expect(no_time.created_at).to be_within(1.minute).of(Time.current)
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

    it 'is idempotent per CSP session and updates the row' do
      described_class.record!(csp:, verification: failure)
      later = { status: 'manual_success', outcome: :verified, session_id: 'verify_abc123',
                created_at:, completed_at: }
      expect { described_class.record!(csp:, verification: later) }.not_to(change { described_class.count })
      expect(described_class.last.status).to eq 'manual_success'
    end

    it 'does not overwrite who the row is linked to' do
      user = create(:user)
      described_class.record!(csp:, verification: failure, user:)
      described_class.record!(csp:, verification: failure, user: create(:user), invitation: create(:invitation, :ao))
      record = described_class.last
      expect(record.user).to eq user
      expect(record.invitation).to be_nil
    end

    it 'replaces checks when new ones are given and keeps them when not' do
      described_class.record!(csp:, verification: failure)
      described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_abc123',
                                                    checks: [checks.first] })
      expect(CspVerificationCheck.count).to eq 1

      described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_abc123' })
      expect(CspVerificationCheck.count).to eq 1
    end

    it 'does not clear the times when a later write lacks them' do
      described_class.record!(csp:, verification: failure)
      described_class.record!(csp:, verification: { status: 'fail', session_id: 'verify_abc123' })
      expect(described_class.last).to have_attributes(created_at:, completed_at:)
    end

    it 'keeps sessions from different CSPs apart' do
      other = create(:csp, :id_me)
      described_class.record!(csp:, verification: failure)
      expect { described_class.record!(csp: other, verification: failure) }.to change { described_class.count }.by(1)
    end

    it 'retries once if another writer inserts the same session first' do
      attempts = 0
      # In production the unique index rejects our insert at save!, after the lookup found nothing
      allow_any_instance_of(described_class).to receive(:save!).and_wrap_original do |original, *args|
        attempts += 1
        raise ActiveRecord::RecordNotUnique, 'duplicate' if attempts == 1

        original.call(*args)
      end

      expect(described_class.record!(csp:, verification: failure)).to be_persisted
      expect(attempts).to eq 2
      expect(described_class.where(csp_verification_id: 'verify_abc123').count).to eq 1
    end

    it 'requires a session id' do
      expect { described_class.record!(csp:, verification: { status: 'fail' }) }
        .to raise_error(ArgumentError, /session_id/)
    end
  end

  it 'requires a session id and status' do
    expect(described_class.new(csp:)).not_to be_valid
  end
end
