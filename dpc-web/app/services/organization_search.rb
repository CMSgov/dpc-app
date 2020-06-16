# frozen_string_literal: true

class OrganizationSearch
  ALLOWED_SCOPES = %w[all provider vendor].freeze

  attr_reader :params, :initial_scope

  def initialize(params: {}, scope: 'all')
    @params = params
    @initial_scope = scope
  end

  def results
    @results ||= query
  end

  private

  def query
    scope = Organization

    scope = apply_org_queries(scope)
    scope = apply_date_queries(scope)
    scope = apply_keyword_search(scope)

    scope
  end

  private

  def apply_org_queries(scope)
    if params[:registered_org]
      radio = params[:registered_org]

      if radio == 'registered'
        scope = scope.where('id IN(SELECT DISTINCT(organization_id) FROM registered_organizations)')
      elsif radio == 'unregistered'
        scope = scope.where('id NOT IN(SELECT DISTINCT(organization_id) FROM registered_organizations)')
      else
        scope = Organization
      end
    end

    # Check if provider or vender
    if params[:org_type] == 'vendor'
      scope = scope.vendor
    elsif params[:org_type] == 'provider'
      scope = scope.provider
    end

    # Check org type
    if params[:organization_type].present?
      scope = scope.where(organization_type: params[:organization_type])
    end

    scope
  end

  def apply_date_queries(scope)
    if params[:created_after].present?
      scope = scope.where('organizations.created_at > :created_after', created_after: params[:created_after])
    end

    if params[:created_before].present?
      scope = scope.where('organizations.created_at < :created_before', created_before: params[:created_before])
    end

    scope
  end

  def apply_keyword_search(scope)
    if params[:keyword].present?
      keyword = "%#{params[:keyword].downcase}%"
      scope = scope.where('LOWER(name) LIKE :keyword', keyword: keyword)
    end

    scope
  end
end
