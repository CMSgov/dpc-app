# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CredentialAuditLog, type: :model do
  context :validations do
    let(:log) { build(:credential_audit_log) }
    it 'should pass with factory' do
      expect(log).to be_valid
    end

    it 'fails if no user' do
      log.user = nil
      expect(log).to_not be_valid
      expect(log.errors.size).to eq(1), log.errors.inspect
      expect(log.errors[:user]).to eq ['must exist']
    end

    it 'fails if no credential type' do
      log.credential_type = nil
      expect(log).to_not be_valid
      expect(log.errors.size).to eq(1), log.errors.inspect
      expect(log.errors[:credential_type]).to eq ["can't be blank"]
    end

    it 'fails on invalid credential type' do
      expect do
        log.credential_type = :wasp_compliant
      end.to raise_error(ArgumentError)
    end

    it 'fails if no dpc_api_credential_id' do
      log.dpc_api_credential_id = nil
      expect(log).to_not be_valid
      expect(log.errors.size).to eq(1), log.errors.inspect
      expect(log.errors[:dpc_api_credential_id]).to eq ["can't be blank"]
    end

    it 'fails if no action' do
      log.action = nil
      expect(log).to_not be_valid
      expect(log.errors.size).to eq(1), log.errors.inspect
      expect(log.errors[:action]).to eq ["can't be blank"]
    end

    it 'fails on invalid action' do
      expect do
        log.action = :finagle
      end.to raise_error(ArgumentError)
    end
  end
end
