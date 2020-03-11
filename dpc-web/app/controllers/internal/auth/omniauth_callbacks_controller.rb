# frozen_string_literal: true

module Internal
  module Auth
    class OmniauthCallbacksController < Devise::OmniauthCallbacksController
      include InternalUserDeviseHelper
      include MultiModelLoginHelper
      # You should configure your model like this:
      # devise :omniauthable, omniauth_providers: [:twitter]

      def oktaoauth
        if authorized_internal_user?
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

      # TODO: Right now this means the email domain must be cms.hhs.gov (or mine) and should be changed.
      def authorized_internal_user?
        email = request.env['omniauth.auth']['info']['email']
        email.match(/@cms.hhs.gov\z/) || email == 'shelbyswitzer@gmail.com'
      end
    end
  end
end
