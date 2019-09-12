# frozen_string_literal: true

module DeviseHelper
  def devise_error_messages!
    return if flash.notice.blank? && flash.alert.blank? || flash.notice =~ /signed out/i
    return notice_msg.html_safe if flash.notice

    incorrect_email_or_password_msg.html_safe
  end

  def notice_msg
    <<-HTML
      <div class="ds-c-alert ds-u-margin-bottom--5">
        <div class="ds-c-alert__body">
          <h3 class="ds-c-alert__heading">#{flash.notice}</h3>
        </div>
      </div>
    HTML
  end

  def incorrect_email_or_password_msg
    <<-HTML
    <div class="ds-c-alert ds-c-alert--error">
      <div class="ds-c-alert__body">
        <h3 class="ds-c-alert__heading">Your email or password is incorrect.</h3>
      </div>
    </div>
    HTML
  end
end
