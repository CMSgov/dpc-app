# frozen_string_literal: true

class ApplicationMailer < ActionMailer::Base
  default from: 'dpcinfo@cms.hhs.gov'
  layout 'mailer'
end
