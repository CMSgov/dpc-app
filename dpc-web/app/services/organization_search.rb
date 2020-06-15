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
    scope = Organization

    scope = apply_org_queries(scope)
    # scope = apply_date_queries(scope)
    # scope = apply_keyword_search(scope)

    scope
  end

  def apply_org_queries(scope)

    # Check if provider or vender
    if params[:org_type] == 'vendor'
      scope = scope.vendor
    elsif params[:org_type] == 'provider'
      scope = scope.provider
    end

    # Check if registered org
    # if params[:]
    #   scope
    # elsif
    # end

    # Check if keyword is registered org

    # Check on org type
    if params[:requested_org_type].present?
      scope = scope.where(organization_type: params[:requested_org_type])
    end

    scope
  end

  # def apply_keyword_search(scope)
  #   if keyword_param[:keyword].present?
  #     keyword = "%#{keyword_param[:keyword].downcase}%"
  #     scope = scope.where('LOWER(name) LIKE :keyword', keyword: keyword)
  #   end

  #   scope
  # end
end
