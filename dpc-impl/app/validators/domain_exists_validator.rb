# frozen_string_literal: true

require 'mail'
class DomainExistsValidator < ActiveModel::EachValidator#add
  def validate_each(record, attribute, value)
    begin
      r = Truemail.validate(value).result.success
    rescue StandardError
      r = false
    end
    record.errors.add attribute, (options[:message] || ' is invalid') unless r
  end
end
