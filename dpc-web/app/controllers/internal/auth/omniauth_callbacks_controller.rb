# frozen_string_literal: true

module Internal
  module Auth
    class OmniauthCallbacksController < Devise::OmniauthCallbacksController
      include InternalUserDeviseHelper
      include MultiModelLoginHelper
      # You should configure your model like this:
      # devise :omniauthable, omniauth_providers: [:twitter]

      def oktaoauth
        if InternalUser::OKTA_AUTH_ENABLED
          if request.env['omniauth.auth'].nil?
            redirect_to new_internal_user_session_path, alert: 'Failed request.'
          elsif authorized_internal_user?
            @internal_user = InternalUser.from_omniauth(request.env['omniauth.auth'])
            flash[:notice] = "You have signed in as #{@internal_user.email} with IDM Hub."
            sign_in_and_redirect @internal_user
          else
            redirect_to new_internal_user_session_path, alert: formatted_auth_errors
          end
        else
          redirect_to new_internal_user_session_path, alert: 'Not allowed.'
        end
      end

      def github
        redirect_to new_internal_user_session_path unless InternalUser::GITHUB_AUTH_ENABLED

        if valid_org_team?
          @internal_user = InternalUser.from_omniauth(request.env['omniauth.auth'])
          flash[:notice] = "You have signed in as #{@internal_user.email || @internal_user.name} with Github."
          sign_in_and_redirect @internal_user
        else
          redirect_to new_internal_user_session_path, error: formatted_auth_errors
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

      def formatted_auth_errors
        auth_errors.join(', ')
      end

      def authorized_internal_user?
        raw_info = request.env['omniauth.auth']['extra']['raw_info']
        admin_role = ENV.fetch('OKTA_ADMIN_ROLE')

        unless raw_info['Roles'].include? admin_role
          auth_errors << 'Must have admin role.'
        end

        unless raw_info['LOA'] == '3'
          auth_errors << 'Must have LOA 3.'
        end
        auth_errors.empty?
      end

      def valid_org_team?
        valid = github_client.user_teams.any? do |team|
          team[:id].to_s == ENV.fetch('GITHUB_ORG_TEAM_ID')
        end

        unless valid
          auth_errors << 'Must have valid Github Org Team.'
        end
        auth_errors.empty?
      end

      def auth_errors
        @auth_errors ||= []
      end

      def github_client
        @github_client ||= Octokit::Client.new(
          access_token: request.env['omniauth.auth']['credentials']['token']
        )
      end
    end
  end
end
