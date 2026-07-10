# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CspSession do
  let(:session) { ActiveSupport::HashWithIndifferentAccess.new }
  subject(:csp_session) { described_class.new(session) }

  let(:token) { 'bearer-token' }
  let(:future_exp) { 1.hour.from_now }
  let(:past_exp)   { 1.hour.ago }

  describe '#store' do
    it 'accepts a symbol csp and normalizes to string' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      expect(csp_session.current).to eq 'login_dot_gov'
    end

    it 'accepts a string csp' do
      csp_session.store(csp: 'id_me', token: token, token_exp: future_exp)
      expect(csp_session.current).to eq 'id_me'
    end

    it 'stores token data and marks the CSP current' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      expect(csp_session.current).to eq 'login_dot_gov'
      expect(csp_session.token).to eq token
      expect(csp_session.token_exp).to eq future_exp
    end

    it 'replaces existing data for the same CSP' do
      csp_session.store(csp: :login_dot_gov, token: 'old', token_exp: 1.minute.from_now)
      csp_session.store(csp: :login_dot_gov, token: 'new', token_exp: future_exp)
      expect(csp_session.token(:login_dot_gov)).to eq 'new'
      expect(csp_session.token_exp(:login_dot_gov)).to eq future_exp
    end

    it 'keeps other CSPs untouched' do
      csp_session.store(csp: :id_me, token: 'a', token_exp: future_exp)
      csp_session.store(csp: :login_dot_gov, token: 'b', token_exp: future_exp)
      expect(csp_session.token(:id_me)).to eq 'a'
      expect(csp_session.current).to eq 'login_dot_gov'
    end
  end

  describe '#activate' do
    it 'switches the current CSP without changing tokens' do
      csp_session.store(csp: :id_me, token: 'a', token_exp: future_exp)
      csp_session.store(csp: :login_dot_gov, token: 'b', token_exp: future_exp)
      csp_session.activate(:id_me)
      expect(csp_session.current).to eq 'id_me'
      expect(csp_session.token).to eq 'a'
    end
  end

  describe 'user helpers' do
    it 'stores, reads, and clears the user' do
      user = 1
      csp_session.store_user(user)
      expect(csp_session.user).to eq user
      csp_session.clear_user
      expect(csp_session.user).to be_nil
    end
  end

  describe '#inactive_reason' do
    it 'returns :no_session when no CSP is current' do
      expect(csp_session.inactive_reason).to eq :no_session
    end

    it 'returns :no_token when token is missing' do
      csp_session.store(csp: :login_dot_gov, token: nil, token_exp: future_exp)
      expect(csp_session.inactive_reason).to eq :no_token
    end

    it 'returns :no_token_exp when expiration is missing' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: nil)
      expect(csp_session.inactive_reason).to eq :no_token_exp
    end

    it 'returns :expired_token when expiration is in the past' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: past_exp)
      expect(csp_session.inactive_reason).to eq :expired_token
    end

    it 'returns nil when the session is active' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      expect(csp_session.inactive_reason).to be_nil
    end

    it 'inspects the given CSP, not just the current one' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.store(csp: :id_me, token: token, token_exp: past_exp)
      csp_session.activate(:login_dot_gov)
      expect(csp_session.inactive_reason(:id_me)).to eq :expired_token
    end
  end

  describe '#active?' do
    it 'is true only when inactive_reason is nil' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      expect(csp_session.active?).to be true
    end

    it 'is false for expired tokens' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: past_exp)
      expect(csp_session.active?).to be false
    end
  end

  describe '#active_csps / #any_active?' do
    it 'lists only unexpired CSPs' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.store(csp: :id_me,         token: token, token_exp: past_exp)
      expect(csp_session.active_csps).to contain_exactly('login_dot_gov')
      expect(csp_session.any_active?).to be true
    end

    it 'returns empty and false when nothing is active' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: past_exp)
      expect(csp_session.active_csps).to be_empty
      expect(csp_session.any_active?).to be false
    end
  end

  describe '#stored?' do
    it 'reports whether a CSP has been stored, active or not' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: past_exp)
      expect(csp_session.stored?(:login_dot_gov)).to be true
      expect(csp_session.stored?(:id_me)).to be false
    end
  end

  describe '#clear' do
    it 'removes the given CSP and falls back to another active CSP when it was current' do
      csp_session.store(csp: :id_me,         token: token, token_exp: future_exp)
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.clear(:login_dot_gov)
      expect(csp_session.stored?(:login_dot_gov)).to be false
      expect(csp_session.current).to eq 'id_me'
    end

    it 'sets current to nil when no other active CSP remains' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.clear(:login_dot_gov)
      expect(csp_session.current).to be_nil
    end

    it 'leaves current alone when clearing a non-current CSP' do
      csp_session.store(csp: :id_me,         token: token, token_exp: future_exp)
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.clear(:id_me)
      expect(csp_session.current).to eq 'login_dot_gov'
    end

    it 'falls back on the last added csp when clearing the current csp' do
      csp_session.store(csp: :id_me,         token: token, token_exp: future_exp)
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.store(csp: :clear,         token: token, token_exp: future_exp)
      csp_session.clear(:clear)
      expect(csp_session.current).to eq 'login_dot_gov'
    end
  end

  describe '#clear_all' do
    it 'wipes both current and stored sessions' do
      csp_session.store(csp: :login_dot_gov, token: token, token_exp: future_exp)
      csp_session.store(csp: :id_me,         token: token, token_exp: future_exp)
      csp_session.clear_all
      expect(csp_session.current).to be_nil
      expect(csp_session.active_csps).to be_empty
    end
  end
end
