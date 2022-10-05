# frozen_string_literal: true

require 'mail'
class DomainExistsValidator < ActiveModel::EachValidator
  def validate_each(record, attribute, value)
    begin
      r = Truemail.validate(value, with: :mx).result.success
    rescue StandardError
      r = false
    end
    record.errors[attribute] << (options[:message] || 'is invalid') unless r
  end
end
