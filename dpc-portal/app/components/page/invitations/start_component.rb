# frozen_string_literal: true

module Page
  module Invitations
    # First page the user sees when accepting a valid invitation
    class StartComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
        @name = "#{invitation&.invited_given_name} #{invitation&.invited_family_name}"
        @hours, @minutes = invitation.expires_in
        @expiration = @hours.positive? ? pluralize(@hours, 'hour') : pluralize(@minutes, 'minute')
        @list_styles = %i[text-green usa-media-block__img]
        @musts = if @invitation.authorized_official?
                   [
                     'Be an active AO of your organization',
                     'Not be listed on the Medicare Exclusions Database (or your organization)',
                     'Be registered in the Provider Enrollment, Chain, and Ownership System (PECOS)'
                   ]
                 else
                   [
                     'Verify your identity with Login.gov',
                     'Use the same email address the invite was sent to',
                     'Make sure the name you sign up with matches the one shown on this screen'
                   ]
                 end
      end
    end
  end
end
