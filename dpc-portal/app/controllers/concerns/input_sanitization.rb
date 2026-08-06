# frozen_string_literal: true

# to prevent path traversal and injection vulnerabilities.
module InputSanitization
  extend ActiveSupport::Concern

  private

  def sanitize_uid(id_param)
    return nil if id_param.blank?

    sanitized = id_param.to_s.strip
    sanitized if sanitized.match?(/\A[a-zA-Z0-9-]{1,64}\z/)
  end
end
