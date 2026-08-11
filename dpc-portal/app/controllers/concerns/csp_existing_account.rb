# frozen_string_literal: true

# Handles merging existing emails and accounts
module CspExistingAccount
  extend ActiveSupport::Concern

  private

  def handle_csp_user_response(csp_user, auth)
    return render_add_email(csp_user, auth) if csp_user && !email_match?(csp_user, auth)

    orig_csp_user = existing_account(auth)
    return render_link_account(primary_email(auth), orig_csp_user.csp.name) if orig_csp_user.present?

    check_csp_session(auth)
    sync_and_redirect(csp_user, auth)
  end

  def email_match?(csp_user, auth)
    csp_user.user_emails.empty? || csp_user.user_emails.map(&:email).include?(primary_email(auth))
  end

  def name_match?(user, auth)
    info = auth.extra.raw_info
    user.given_name.casecmp?(info.given_name) && user.family_name.casecmp?(info.family_name)
  end

  def existing_account(auth)
    email = UserEmail.includes(csp_user: :user)
                     .where(email: [primary_email(auth), *all_emails(auth)])
                     .find { |email| email.csp_user.csp.name != auth.provider.to_s }
    return nil unless email

    csp_user = email.csp_user
    csp_user if name_match?(csp_user.user, auth)
  end

  def render_add_email(csp_user, auth)
    Rails.logger.info(['User has existing account associated with different email',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountEmail,
                         **csp_log_context }])
    render(Page::ExistingAccount::AddEmailComponent.new(csp_user.user.email, csp_user.csp.name,
                                                        update_path(id: csp_user.id,
                                                                    csp: csp_user.csp.id,
                                                                    all_emails: all_emails(auth),
                                                                    primary_email: primary_email(auth))))
  end

  def render_link_account(email, csp)
    Rails.logger.info(['User has existing account associated with different CSP',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountCsp,
                         csp: }])
    render(Page::ExistingAccount::LinkAccountComponent.new(email, csp))
  end

  def check_csp_session(auth)
    return if csp_session.active_csps.one?

    verify_account_match
    create_csp_user(auth.uid)
  end

  def verify_account_match
    all_user_info = UserInfoService.new.all_user_info(csp_session)
    all_ssns = all_user_info.values.map { |user_info| ssn(user_info) }
    raise 'SSN mismatch' unless all_ssns.uniq.one?
  end

  def ssn(user_info)
    user_info.dig('extra', 'raw_info', 'social_security_number') ||
      user_info.dig('extra', 'raw_info', 'SSN') ||
    user_info.dig('extra', 'raw_info', 'ssn')
  end

  def create_csp_user(uuid)
    csp_session.active_csps.each do |csp_name|
      csp = Csp.find_by(name: csp_name)
      CspUser.find_or_create_by(user: current_user, csp:, uuid:)
    end
  end
end
