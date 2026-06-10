# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponent < ViewComponent::Base
      # TODO: remove default -acw
      def initialize(invitation, reason, csp: :login_dot_gov)
        super()
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @csp_display_name = csp_display_name(csp)
        @reason = resolve_reason(reason, csp)
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end

      private

      def csp_display_name(csp)
        case csp.to_s
        when 'login_dot_gov' then 'Login.gov'
        when 'id_me' then 'ID.me'
        else 'CSP'
        end
      end

      def resolve_reason(reason, csp)
        reason_sym = if AoVerificationService::SERVER_ERRORS.include?(reason)
                       :server_error
                     else
                       reason.to_sym
                     end
        return reason_sym unless reason.to_s.starts_with?('csp_')

        prefix = case csp.to_s
                 when 'login_dot_gov' then 'login_dot_gov_'
                 when 'id_me' then 'id_me_'
                 else 'csp_'
                 end
        reason.to_s.sub('csp_', prefix).to_sym
      end
    end
  end
end
