# app/controllers/concerns/input_sanitization.rb

module InputSanitization
  extend ActiveSupport::Concern

  private

  def sanitize_uid(id_param)
    return nil if id_param.blank?

    sanitized = id_param.to_s.strip
    sanitized if sanitized.match?(/\A[a-zA-Z0-9\-]{1,64}\z/)
  end

  # Allow printable characters and limits length to prevent abuse
  def sanitize_label(label)
    return nil if label.blank?

    sanitized = label.to_s.gsub("\r", '').strip
    sanitized if sanitized.match?(/\A[\w\s\-\.]{1,255}\z/)
  end
end