# frozen_string_literal: true

module Core
  module Alert
    # Alert Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/alert/)
    #
    class Preview < ViewComponent::Preview
      # To create a slim alert, leave the heading blank.
      #
      # @param status [Symbol] select [info, warning, error, success]
      # @param icon toggle
      # @param heading
      # @param body
      def default(status: :info, icon: true, heading: 'Heading',
                  body: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod.')
        render Core::Alert::Component.new(status:, heading:,
                                          include_icon: icon) do
          body
        end
      end
    end
  end
end
