# frozen_string_literal: true

require 'rails_helper'

RSpec.describe UserSearch do
  describe 'allowed scopes' do
    it 'raises error when scope not allowed' do
      expect { UserSearch.new(params: {}, scope: 'bad') }.to raise_error(ArgumentError)
    end
  end

  describe '#results' do
    let!(:user) { create(:user, :assigned, created_at: 5.days.ago) }
    let!(:unassigned_user) { create(:user, created_at: 5.days.ago) }
    let!(:new_user) { create(:user, :assigned, created_at: Time.now) }
    let!(:vendor_user) { create(:user, :vendor, created_at: 5.days.ago) }
    let!(:old_vendor_user) { create(:user, :vendor, created_at: 1.month.ago) }

    describe 'no scope' do
      it 'applies params to search all users' do
        params = {
          org_status: 'assigned',
          created_after: 7.days.ago,
          created_before: 1.day.ago
        }

        expect(UserSearch.new(params: params).results).to match_array([user, vendor_user])
      end
    end

    describe 'assigned_non_vendor scope' do
      it 'applies params to search only non_vendor users' do
        params = {
          org_status: 'assigned',
          created_after: 7.days.ago,
        }

        expect(UserSearch.new(params: params, scope: :assigned_non_vendor).results).to match_array([user, new_user])
      end
    end

    describe 'assigned_vendor scope' do
      it 'applies params to search only vendor users' do
        params = {
          org_status: 'assigned',
          created_after: 7.days.ago,
        }

        expect(UserSearch.new(params: params, scope: :assigned_vendor).results).to match_array([vendor_user])
      end
    end
  end
end
