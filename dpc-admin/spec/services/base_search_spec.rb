# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BaseSearch do
  describe '#initial_scope' do
    it 'sets initial organization scope to all no scope' do
      params = {
        controller: 'organizations',
      }

      expect(BaseSearch.new(params: {}).initial_scope).to eq('all')
    end

    it 'sets initial user scope to all no scope' do
      params = {
        controller: 'users',
      }

      expect(BaseSearch.new(params: {}).initial_scope).to eq('all')
    end

    it 'sets initial scope to scope passed' do
      expect(BaseSearch.new(params: {}, scope: 'vendor').initial_scope).to eq('vendor')
    end
  end

  describe '#organization-results' do
    let!(:first_org) { create(:organization, created_at: 5.days.ago) }
    let!(:second_org) { create(:organization, created_at: 10.days.ago) }
    let!(:third_org) { create(:organization, created_at: Time.now) }

    context 'no scope' do
      it 'returns all organization matching params' do
        params = {
          controller: 'organizations',
        }

        expect(BaseSearch.new(params:).results).to match_array([first_org, second_org, third_org])
      end
    end

    context 'organization date filter' do
      context 'organization with created after' do
        it 'returns organization created after a date' do
          params = {
            controller: 'organizations',
            created_after: 7.days.ago
          }

          expect(BaseSearch.new(params:).results).to match_array([first_org, third_org])
        end
      end

      context 'organization with created before' do
        it 'returns organization created before a date' do
          params = {
            controller: 'organizations',
            created_before: 7.days.ago
          }

          expect(BaseSearch.new(params:).results).to match_array([second_org])
        end
      end
    end
  end

  describe '#user-results' do
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
          controller: 'users',
          org_status: 'assigned',
          created_after: 7.days.ago,
          created_before: 1.day.ago
        }

        expect(BaseSearch.new(params:).results).to match_array([user, vendor_user])
      end
    end

    context 'provider scope' do
      context 'assigned with created_after' do
        it 'returns assigned users created after a date' do
          params = {
            controller: 'users',
            org_status: 'assigned',
            created_after: 7.days.ago,
          }

          expect(BaseSearch.new(params:, scope: 'provider').results).to match_array([user, new_user])
        end
      end

      context 'unassigned with created_before' do
        it 'returns unassigned users created before a date' do
          params = {
            controller: 'users',
            org_status: 'unassigned',
            created_before: 7.days.ago,
          }

          expect(BaseSearch.new(params:, scope: 'provider').results).to match_array([old_unassigned_user])
        end
      end
    end

    context 'vendor scope' do
      context 'assigned with created_before' do
        it 'returns assigned users created before a date' do
          params = {
            controller: 'users',
            org_status: 'assigned',
            created_before: 7.days.ago,
          }

          expect(BaseSearch.new(params:, scope: 'vendor').results).to match_array([old_vendor_user])
        end
      end

      context 'unassigned with created_after' do
        it 'returns unassigned users created after a date' do
          params = {
            controller: 'users',
            org_status: 'unassigned',
            created_after: 7.days.ago,
          }

          expect(BaseSearch.new(params:, scope: 'vendor').results).to match_array([unassigned_vendor_user])
        end
      end
    end
  end
end
