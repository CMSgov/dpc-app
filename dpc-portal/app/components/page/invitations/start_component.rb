# frozen_string_literal: true

module Page
  module Invitations
    # First page the user sees when accepting a valid invitation
    class StartComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
        @hours, @minutes = invitation.expires_in
        @expiration = @hours.positive? ? pluralize(@hours, 'hour') : pluralize(@minutes, 'minute')
        @list_styles = %i[text-green usa-media-block__img]
        @musts = [
          'Be an active AO of your organization',
          'Not be listed on the Medicare Exclusions Database (or your organization)',
          'Be registered in the Provider Enrollment, Chain, and Ownership System (PECOS)'
        ]
      end
    end
  end
end
