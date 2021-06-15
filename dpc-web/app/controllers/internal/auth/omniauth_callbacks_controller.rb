# frozen_string_literal: true

module Internal
  module Auth
    class OmniauthCallbacksController < Devise::OmniauthCallbacksController
      # You should configure your model like this:
      # devise :omniauthable, omniauth_providers: [:twitter]

      def github
        if valid_org_team?
          @internal_user = InternalUser.from_omniauth(request.env['omniauth.auth'])
          flash[:notice] = "You have successfully signed in as #{@internal_user.email || @internal_user.name}"
          sign_in_and_redirect @internal_user
        else
          redirect_to new_internal_user_session_path, error: 'No can do.'
        end
      end

      # More info at:
      # https://github.com/plataformatec/devise#omniauth

      # GET|POST /resource/auth/twitter
      # def passthru
      #   super
      # end

      # GET|POST /users/auth/twitter/callback
      # def failure
      #   super
      # end

      protected

      # The path used when OmniAuth fails
      # def after_omniauth_failure_path_for(scope)
      #   super(scope)
      # end

      def valid_org_team?
        github_client.user_teams.any? do |team|
          team[:id].to_s == ENV.fetch('GITHUB_ORG_TEAM_ID')
        end
      end

      def github_client
        @github_client ||= Octokit::Client.new(
          access_token: request.env['omniauth.auth']['credentials']['token']
        )
      end
    end
  end
end
