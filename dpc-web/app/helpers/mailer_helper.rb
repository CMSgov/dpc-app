# frozen_string_literal: true

module MailerHelper
  def last_email
    ActionMailer::Base.deliveries.last
  end
end
