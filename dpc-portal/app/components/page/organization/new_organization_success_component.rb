# frozen_string_literal: true

module Page
    module Organization
      # Page shown after successful completion of adding new organization.
      class NewOrganizationSuccessComponent < ViewComponent::Base
        attr_accessor :organization
  
        def initialize(organization)
          super
          @organization = organization
        end
      end
    end
  end
  