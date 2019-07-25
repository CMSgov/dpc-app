module DeviseHelper

  def devise_error_messages!
    flash_alerts = []
    error_key = 'errors.messages.not_saved'

    if !flash.empty?
      flash_alerts.push(flash[:error]) if flash[:error]
      flash_alerts.push(flash[:alert]) if flash[:alert]
      flash_alerts.push(flash[:notice]) if flash[:notice]
      error_key = 'devise.failure.invalid'
    end

    return "" if resource.errors.empty? && flash_alerts.empty?
    errors = resource.errors.empty? ? flash_alerts : resource.errors.full_messages



    html = <<-HTML
    <div class="ds-c-alert ds-c-alert--error">
      <div class="ds-c-alert__body">
        <h3 class="ds-c-alert__heading">Your email or password is incorrect.</h3>
        <p class="ds-c-alert__text">If you forgot your password, please email <a href="mailto:dpcinfo@cms.hhs.gov?Subject=Forgot Password">dpcinfo@cms.hhs.gov</a> to request a reset.</p>
      </div>
    </div>
    HTML

    html.html_safe
  end

end