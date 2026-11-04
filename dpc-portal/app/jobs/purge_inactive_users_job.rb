# frozen_string_literal: true

# Remove users after period of inactivity
class PurgeInactiveUsersJob < ApplicationJob
  queue_as :portal
  include StructuredLogging

  RETENTION_PERIOD = 6.years

  def perform
    inactive_users.find_each do |user|
      purge_user(user)
    rescue StandardError => e
      log_failure(user, e)
    end
  end

  private

  def inactive_users
    inactive_user_ids = UserSignIn.group(:user_id)
                                  .having('MAX(last_login_at) < ?', RETENTION_PERIOD.ago)
                                  .pluck(:user_id)
    User.where(id: inactive_user_ids)
  end

  def purge_user(user)
    last_login_at = user.user_sign_ins.maximum(:last_login_at)
    user_id = user.id

    ActiveRecord::Base.transaction do
      detach_references(user)
      delete_csp_user_records(user)
      delete_dependents(user)
      user.destroy!
      UserPurgeLog.create!(user_id: user_id, last_login_at:, purged_at: Time.now)
    end
  end

  def detach_references(user)
    Invitation.where(invited_by_id: user.id).update_all(invited_by_id: nil)
    ProviderOrganization.where(terms_of_service_accepted_by_id: user.id)
                        .update_all(terms_of_service_accepted_by_id: nil)

    CspUserVerification.where(user_id: user.id).update_all(user_id: nil)
    CredentialAuditLog.where(user_id: user.id).update_all(user_id: nil)
  end

  def delete_csp_user_records(user)
    csp_user_ids = CspUser.where(user_id: user.id).select(:id)
    UserEmail.where(csp_user_id: csp_user_ids).delete_all
    CspUser.where(user_id: user.id).delete_all
  end

  def delete_dependents(user)
    AoOrgLink.where(user_id: user.id).delete_all
    CdOrgLink.where(user_id: user.id).delete_all
    UserSignIn.where(user_id: user.id).delete_all
  end

  def log_failure(user, error)
    log_event(:error, 'User purge failed',
              action_context: LoggingConstants::ActionContext::UserManagement,
              action_type: LoggingConstants::ActionType::UserPurgeFailed,
              user_identifier: user.id,
              error: error.class.name)
  end

  def csp_log_context
    {}
  end
end
