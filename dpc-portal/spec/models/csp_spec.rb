# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Csp, type: :model do
  it 'has many csp_users and users' do
    csp = create(:csp)
    user = create(:user)
    csp_user = create(:csp_user, user:, csp:)
    expect(csp.csp_users).to eq [csp_user]
    expect(csp.users).to eq [user]
  end

  context 'csp is active' do
    it 'active scope finds CSP' do
      csp = create(:csp, :login_dot_gov)
      expect(Csp.active.where(name: 'login_dot_gov').last).to eq csp
    end

    it 'active scope finds CSP with end date in the future' do
      csp = create(:csp, :active_with_end_date)
      expect(Csp.active.where(name: 'login_dot_gov').last).to eq csp
    end
  end

  context 'csp is inactive' do
    it 'active scope does not find CSP' do
      create(:csp, :inactive)
      expect(Csp.active.where(name: 'inactive').last).to eq nil
    end
  end
end
