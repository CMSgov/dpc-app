# frozen_string_literal: true

module Internal
  class UsersController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      scope = User.all

      if params[:keyword].present?
        keyword = "%#{params[:keyword].downcase}%"
        scope = scope.where(
          'LOWER(first_name) LIKE :keyword OR LOWER(last_name) LIKE :keyword OR LOWER(email) LIKE :keyword',
          keyword: keyword
        )
      end

      if params[:requested_org].present?
        org = "%#{params[:requested_org].downcase}%"
        scope = scope.where('LOWER(organization) LIKE :org', org: org)
      end

      if params[:requested_org_type].present?
        scope = scope.where(organization_type: params[:requested_org_type])
      end

      if params[:created_after].present?
        scope = scope.where('created_at > :created_after', created_after: params[:created_after])
      end

      if params[:created_before].present?
        scope = scope.where('created_at < :created_before', created_before: params[:created_before])
      end

      @users = scope.order('created_at DESC').page params[:page]
    end

    def show
      @user = User.find(params[:id])
    end
  end
end
