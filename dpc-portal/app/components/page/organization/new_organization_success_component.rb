# frozen_string_literal: true

module Page
    module Organization
      # Page shown after successful completion of adding new organization.
      class NewOrganizationSuccessComponent < ViewComponent::Base
        attr_accessor :npi
  
        def initialize(npi)
          super
          @npi = npi
        end
      end
    end
  end
  