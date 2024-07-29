# frozen_string_literal: true

module Users
  # Adds auto session timeout functions to devise session controller
  class SessionsController < Devise::SessionsController
    auto_session_timeout_actions

    def destroy
      if params[:invitation_id].present?
        invitation = Invitation.find(params[:invitation_id])
        session[:user_return_to] = accept_organization_invitation_url(invitation.provider_organization.id, invitation.id)
      end
      session['omniauth.state'] = @state = SecureRandom.hex(16)
      sign_out(current_user)
      client_id = "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV.fetch('ENV')}"
      url = URI::HTTPS.build(host: ENV.fetch('IDP_HOST'),
                             path: '/openid_connect/logout',
                             query: { client_id:,
                                      post_logout_redirect_uri: "#{root_url}users/auth/logged_out",
                                      state: @state }.to_query)
      redirect_to url, allow_other_host: true
    end
  end
end
