# frozen_string_literal: true

require 'active_support/concern'

module ApiErrorSimplify
  extend ActiveSupport::Concern

  def api_simplify(err)
    issue = err["issue"]
    binding.pry
    details = issue.pop
    err = details["details"]["text"]
    return err
  end
end
