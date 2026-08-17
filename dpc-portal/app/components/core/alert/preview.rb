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

      # To source the heading and body from the i18n translation files, use the message_key parameter.
      #
      # @param status [Symbol] select [info, warning, error, success]
      # @param icon toggle
      # @param message_key
      def keyed_message(status: :error, icon: true,
                        message_key: 'email_not_found', csp_display_name: 'Login.gov')
        render Core::Alert::Component.new(status:, message_key:,
                                          include_icon: icon, csp_display_name:)
      end
    end
  end
end
