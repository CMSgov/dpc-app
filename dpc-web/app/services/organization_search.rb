# frozen_string_literal: true

class OrganizationSearch
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
    scope = Organization.send(initial_scope)

    scope = apply_org_queries(scope)
    scope = apply_date_queries(scope)
    scope = apply_org_type(scope)
    scope = apply_keyword_search(scope)

    scope
  end

  def apply_org_queries(scope)
    if params[:org_type] == 'vendor'
      scope = scope.vendor
    elsif params[:org_type] == 'provider'
      scope = scope.provider
    end

    scope
  end

  def apply_org_type(scope)
    if params[:organization_type].present?
      scope = scope.where(organization_type: params[:organization_type])
    end

    scope
  end
end
