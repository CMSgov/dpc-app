# frozen_string_literal: true

module Core
    module Card
        # Organization Card Component
        # ----------------
        #
        # [See at USWDS](https://designsystem.digital.gov/components/card/)
        #
        class OrganizationCardComponentPreview < ViewComponent::Preview
            # @param name "Name of the organization"
            # @param npi "NPI of the organization"
            def good_status(name: "Org Name", npi: "npi_123_abc")
                render(Core::Card::OrganizationCardComponent.new(name: name, npi: npi, status: 'Good', status_color: 'LightSkyBlue'))
            end

            # @param name "Name of the organization"
            # @param npi "NPI of the organization"
            def bad_status(name: "Org Name", npi: "npi_123_abc")
                render(Core::Card::OrganizationCardComponent.new(name: name, npi: npi, status: 'Bad', status_color: 'Red'))
            end
        end
    end
end
