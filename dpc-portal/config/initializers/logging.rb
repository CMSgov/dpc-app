# frozen_string_literal: true

require './app/lib/dpc_json_logger'

Rails.application.configure do
  unless ENV['DISABLE_JSON_LOGGER'] == 'true'
    Rails.logger = DpcJsonLogger.new($stdout)
    config.logger = Rails.logger
    config.logger.formatter = DpcJsonLogger.formatter
    config.log_formatter = DpcJsonLogger.formatter
  end
end

module LoggingConstants
  module ActionContext
    Registration = 'Registration'
    Authentication = 'Authentication'
  end

  module ActionType
    AoInvited = 'AoInvited'
    CdInvited = 'CdInvited'
    AoInvitationExpired = 'AoInvitationExpired'
    CdInvitationExpired = 'CdInvitationExpired'
    AoRenewedExpiredInvitation = 'AoRenewedExpiredInvitation'
    AoSignedToS = 'AoSignedToS'
    AoCreated = 'AoCreated'
    CdCreated = 'CdCreated'
    AoLinkedToOrg = 'AoLinkedToOrg'
    CdLinkedToOrg = 'CdLinkedToOrg'


    UserLoggedIn = 'UserLoggedIn'
    UserLoggedOut = 'UserLoggedOut'
    SessionTimedOut = 'SessionTimedOut'
    UserCancelledLogin = 'UserCancelledLogin'
    FailedInvitationFlow = 'FailedInvitationFlow'
    UserLoginWithoutAccount = 'UserLoginWithoutAccount'
  end
end