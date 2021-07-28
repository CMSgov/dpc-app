# frozen_string_literal: true

require 'active_support/concern'

module ApiErrorSimplify
  extend ActiveSupport::Concern

  def api_simplify(err)
    return err
  end
end
