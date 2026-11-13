# frozen_string_literal: true

# Remove users after period of inactivity
class PurgeInactiveUsersJob < ApplicationJob
  queue_as :portal

  def perform
    inactive_user_ids.each do |user_id|
      purge_user(user_id)
    rescue StandardError => e
      log_failure(user_id, e)
    end
  end

  private

  def retention_cutoff
    6.years.ago
  end

  def inactive_user_ids
    UserSignIn.group(:user_id)
              .having('MAX(last_login_at) < ?', retention_cutoff)
              .pluck(:user_id)
  end

  def purge_user(user_id)
    user = User.find_by(id: user_id)
    return unless user

    user.with_lock do
      next if user.user_sign_ins.exists?(['last_login_at >= ?', retention_cutoff])

      last_login_at = user.user_sign_ins.maximum(:last_login_at)
      delete_org_links(user)
      detach_references(user)
      user.destroy!
      UserPurgeLog.create!(user_id: user_id, last_login_at:, purged_at: Time.now)
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
    Rails.logger.error(['User purge failed',
                        { actionContext: LoggingConstants::ActionContext::UserManagement,
                          actionType: LoggingConstants::ActionType::UserPurgeFailed,
                          user_identifier: user_id,
                          error: error.message }])
  end
end
