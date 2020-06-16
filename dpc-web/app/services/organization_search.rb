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
    scope = apply_registered_query(scope)
    scope = apply_date_queries(scope)
    scope = apply_keyword_search(scope)
    scope = apply_search_word(scope)

    scope
  end

  def apply_org_queries(scope)
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

  def apply_registered_query(scope)
    if params[:registered_org]
      if params[:registered_org] == 'registered'
        scope = scope.where('id IN(SELECT DISTINCT(organization_id) FROM registered_organizations)')
      elsif params[:registered_org] == 'unregistered'
        scope = scope.where('id NOT IN(SELECT DISTINCT(organization_id) FROM registered_organizations)')
      else
        scope = Organization
      end
    end
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

  def apply_search_word(scope)
    if params[:search_word].present?
      search_word = "%#{params[:search_word].downcase}%"
      scope = scope.where('LOWER(name) LIKE :search_word', search_word: search_word)
    end

    scope
  end
end
