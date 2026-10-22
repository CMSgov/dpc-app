# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspVerificationCheck, type: :model do
  it 'belongs to a verification and needs a name' do
    check = create(:csp_verification_check)
    expect(check.csp_user_verification).to be_a(CspUserVerification)
    expect(described_class.new(csp_user_verification: check.csp_user_verification)).not_to be_valid
  end

  it 'is removed with its verification' do
    check = create(:csp_verification_check)
    expect { check.csp_user_verification.destroy }.to change { described_class.count }.by(-1)
  end
end
