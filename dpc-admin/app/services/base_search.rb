# frozen_string_literal: true

class BaseSearch
  ALLOWED_SCOPES = %w[all provider vendor].freeze

  attr_reader :params, :initial_scope

  def initialize(params: {}, scope: 'all')
    scope = 'all' unless ALLOWED_SCOPES.include? scope

    @params = params
    @initial_scope = scope
  end

  def results
    @results ||= query
  end

  private

  def query
    scope = apply_controller(scope)

    scope = apply_org_queries(scope)

    scope
  end

  # General queries
  def apply_controller(scope)
    if params[:controller] == 'organizations'
      scope = Organization.send(initial_scope)

      scope = apply_date_queries(scope, 'organizations')
      scope = apply_org_status(scope)
      scope = apply_org_keyword_search(scope)
    elsif params[:controller] == 'users'
      scope = User.includes(organization_user_assignments: :organization).send(initial_scope)

      scope = apply_date_queries(scope, 'users')
      scope = apply_user_keyword_search(scope)
      scope = apply_user_queries(scope)
    end

    scope = apply_tag_queries(scope)

    scope
  end

  def apply_date_queries(scope, table)
    if params[:created_after].present?
      scope = scope.where(table + '.created_at > :created_after', created_after: params[:created_after])
    end

    if params[:created_before].present?
      scope = scope.where(table + '.created_at < :created_before', created_before: params[:created_before])
    end

    scope
  end

  def apply_org_queries(scope)
    if params[:org_type] == 'vendor'
      scope = scope.vendor
    elsif params[:org_type] == 'provider'
      scope = scope.provider
    end

    if params[:organization_type].present?
      scope = scope.where(organization_type: params[:organization_type])
    end

    scope
  end

  def apply_tag_queries(scope)
    if params[:tags].present?
      scope = scope.with_tags(params[:tags].values)
    end

    scope
  end

  # Organization queries
  def apply_org_keyword_search(scope)
    if params[:keyword].present?
      keyword = "%#{params[:keyword].downcase}%"
      scope = scope.where('LOWER(name) LIKE :keyword', keyword:)
    end

    scope
  end

  def apply_org_status(scope)
    if params[:registered_org] == 'registered'
      scope = scope.is_registered
    elsif params[:registered_org] == 'unregistered'
      scope = scope.is_not_registered
    end

    scope
  end

  # User queries
  def apply_user_keyword_search(scope)
    if params[:keyword].present?
      scope = scope.by_keyword(params[:keyword])
    end

    scope
  end

  def apply_user_queries(scope)
    if params[:org_status] == 'unassigned'
      scope = scope.unassigned
    elsif params[:org_status] == 'assigned'
      scope = scope.assigned
    end

    if params[:requested_org].present?
      org = "%#{params[:requested_org].downcase}%"
      scope = scope.where('LOWER(users.requested_organization) LIKE :org', org:)
    end

    if params[:requested_org_type].present?
      scope = scope.where(requested_organization_type: params[:requested_org_type])
    end

    scope
  end
end
