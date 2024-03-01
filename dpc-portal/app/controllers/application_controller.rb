# frozen_string_literal: true

# Base class for all controllers
class ApplicationController < ActionController::Base
  protected

  def load_organization
    api_org_id = case ENV.fetch('ENV', nil)
                 when 'prod-sbx'
                   return redirect_to root_url
                 when 'test'
                   '6a1dbf47-825b-40f3-b81d-4a7ffbbdc270'
                 when 'dev'
                   '78d02106-2837-4d07-8c51-8d73332aff09'
                 else
                   params[:organization_id] || params[:id]
                 end
    find_or_create_organization(api_org_id)
  end

  def find_or_create_organization(api_org_id)
    @organization = ProviderOrganization.find_or_create_by(dpc_api_organization_id: api_org_id) do |org|
      api_org = org.api_org
      org.name = api_org.name
      org.npi = api_org.identifier.select { |id| id.system == 'http://hl7.org/fhir/sid/us-npi' }.first&.value
    end
  rescue DpcRecordNotFound
    render file: "#{Rails.root}/public/404.html", layout: false, status: :not_found
  end
end
