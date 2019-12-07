# frozen_string_literal: true

require 'rails_helper'

RSpec.describe UserSearch do
  describe '#initial_scope' do
    it 'sets initial scope to all no scope provided' do
      expect(UserSearch.new(params: {}).initial_scope).to eq('all')
    end

    it 'sets initial scope to scope passed' do
      expect(UserSearch.new(params: {}, scope: 'vendor').initial_scope).to eq('vendor')
    end
  end

  describe '#results' do
    let!(:user) { create(:user, :assigned, created_at: 5.days.ago) }
    let!(:unassigned_user) { create(:user, created_at: 5.days.ago) }
    let!(:old_unassigned_user) { create(:user, created_at: 10.days.ago) }
    let!(:new_user) { create(:user, :assigned, created_at: Time.now) }
    let!(:vendor_user) { create(:user, :vendor, created_at: 5.days.ago) }
    let!(:old_vendor_user) { create(:user, :vendor, created_at: 1.month.ago) }
    let!(:unassigned_vendor_user) { create(:user, created_at: 1.day.ago, requested_organization_type: 'health_it_vendor') }

    context 'no scope' do
      it 'returns all users matching params' do
        params = {
          org_status: 'assigned',
          created_after: 7.days.ago,
          created_before: 1.day.ago
        }

        expect(UserSearch.new(params: params).results).to match_array([user, vendor_user])
      end
    end

    context 'provider scope' do
      context 'assigned with created_after' do
        it 'returns assigned users created after a date' do
          params = {
            org_status: 'assigned',
            created_after: 7.days.ago,
          }

          expect(UserSearch.new(params: params, scope: 'provider').results).to match_array([user, new_user])
        end
      end

      context 'unassigned with created_before' do
        it 'returns unassigned users created before a date' do
          params = {
            org_status: 'unassigned',
            created_before: 7.days.ago,
          }

          expect(UserSearch.new(params: params, scope: 'provider').results).to match_array([old_unassigned_user])
        end
      end
    end

    context 'vendor scope' do
      context 'assigned with created_before' do
        it 'returns assigned users created before a date' do
          params = {
            org_status: 'assigned',
            created_before: 7.days.ago,
          }

          expect(UserSearch.new(params: params, scope: 'vendor').results).to match_array([old_vendor_user])
        end
      end

      context 'unassigned with created_after' do
        it 'returns unassigned users created after a date' do
          params = {
            org_status: 'unassigned',
            created_after: 7.days.ago,
          }

          expect(UserSearch.new(params: params, scope: 'vendor').results).to match_array([unassigned_vendor_user])
        end
      end
    end
  end
end
