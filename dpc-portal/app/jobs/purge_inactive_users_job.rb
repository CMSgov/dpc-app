# frozen_string_literal: true

# Remove users after period of inactivity
class PurgeInactiveUsersJob < ApplicationJob
  queue_as :portal
  include StructuredLogging

  RETENTION_PERIOD = 6.years

  def perform
    cutoff = RETENTION_PERIOD.ago
    inactive_user_ids(cutoff).each do |user_id|
      purge_user(user_id, cutoff)
    rescue StandardError => e
      log_failure(user_id, e)
    end
  end

  private

  def inactive_user_ids(cutoff)
    UserSignIn.group(:user_id)
              .having('MAX(last_login_at) < ?', cutoff)
              .pluck(:user_id)
  end

  def purge_user(user_id)
    user = User.find_by(id: user_id)
    return unless user

    user.with_lock do
      next if user.user_sign_ins.exists?(['last_login_at >= ?', RETENTION_PERIOD.ago])

      last_login_at = user.user_sign_ins.maximum(:last_login_at)
      delete_org_links(user)
      detach_references(user)
      user.destroy!
      UserPurgeLog.create!(purged_user_id: user_id, last_login_at:, purged_at: Time.now)
    end
  end

  def delete_org_links(user)
    AoOrgLink.where(user_id: user.id).delete_all
    CdOrgLink.where(user_id: user.id).delete_all
  end

  def detach_references(user)
    Invitation.where(invited_by_id: user.id).update_all(invited_by_id: nil)
    ProviderOrganization.where(terms_of_service_accepted_by_id: user.id)
                        .update_all(terms_of_service_accepted_by_id: nil)
  end

  def log_failure(user_id, error)
    log_event(:error, 'User purge failed',
              action_context: LoggingConstants::ActionContext::UserManagement,
              action_type: LoggingConstants::ActionType::UserPurgeFailed,
              user_identifier: user_id,
              error: error.class.name)
  end

  def csp_log_context
    {}
  end
end
