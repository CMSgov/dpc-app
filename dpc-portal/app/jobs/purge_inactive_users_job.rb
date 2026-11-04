# frozen_string_literal: true

# Remove users after period of inactivity
class PurgeInactiveUsersJob < ApplicationJob
  queue_as :portal
  include StructuredLogging

  RETENTION_PERIOD = 6.years

  def perform
    cutoff = RETENTION_PERIOD.ago
    inactive_user_ids(cutoff).find_each do |user_id|
      purge_user(user_id, cutoff)
    rescue StandardError => e
      log_failure(user, e)
    end
  end

  private

  def inactive_user_ids(cutoff)
    UserSignIn.group(:user_id)
              .having('MAX(last_login_at) < ?', cutoff)
              .pluck(:user_id)
  end

  def purge_user(user_id, cutoff)
    User.new(id: user_id).with_lock do |user|
      next if user.user_sign_ins.exists?(['last_login_at >= ?', cutoff])

      last_login_at = user.user_sign_ins.maximum(:last_login_at)
      delete_org_links(user)
      detach_references(user)
      user.destroy!
      UserPurgeLog.create!(user_id: user_id, last_login_at:, purged_at: Time.now)
    end
  rescue ActiveRecord::RecordNotFound # record has already been deleted
    nil
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
