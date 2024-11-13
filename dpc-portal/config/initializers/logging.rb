# frozen_string_literal: true

require './app/lib/dpc_json_logger'

module LoggingConstants
  module ActionContext
    Registration = 'Registration'
    Authentication = 'Authentication'
    BatchVerificationCheck = 'BatchVerificationCheck'
    HealthCheck = 'HealthCheck'
  end

  module ActionType
    AoInvitationFlowStarted = 'AoInvitationFlowStarted'
    CdInvitationFlowStarted = 'CdInvitationFlowStarted'
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

    BeginLogin = 'BeginLogin'
    UserLoggedIn = 'UserLoggedIn'
    UserLoggedOut = 'UserLoggedOut'
    CdConfirmed = 'CdConfirmed'
    SessionTimedOut = 'SessionTimedOut'
    UserCancelledLogin = 'UserCancelledLogin'
    FailedLogin = 'FailedLogin'
    UserLoginWithoutAccount = 'UserLoginWithoutAccount'

    FailAoPiiCheck = 'FailAoPiiCheck'
    FailCdPiiCheck = 'FailCdPiiCheck'
    FailCpiApiGwCheck = 'FailCpiApiGatewayCheck'
    ApiBlocked = 'ApiBlocked'
    AoHasWaiver = 'AoHasWaiver'
    OrgHasWaiver = 'OrgHasWaiver'

    HealthCheckPassed = 'HealthCheckPassed'
    HealthCheckFailed = 'HealthCheckFailed'
  end
end
