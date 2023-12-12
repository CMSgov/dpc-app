# frozen_string_literal: true

# Utilities for views
module ApplicationHelper
  def formatted_datestr(str)
    return 'No date' if str.blank?

    datetime = DateTime.strptime(str, '%Y-%m-%dT%H:%M:%S')
    datetime.strftime('%m/%d/%Y at %l:%M%p UTC')
  end
end
