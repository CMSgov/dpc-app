# frozen_string_literal: true

class CdOrgLink < ApplicationRecord
  attr_reader :user_id, :provider_organization_id, :invitation_id

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true
  belongs_to :invitation, required: true
end
