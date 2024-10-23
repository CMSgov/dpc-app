# frozen_string_literal: true

# Shared functions of credential managers
module CredentialManager
  extend ActiveSupport::Concern
  attr_reader :api_id, :errors

  SERVER_ERROR_MSG = "We're sorry, but we can't complete your request. Please try again tomorrow."

  included do
    def initialize(api_id)
      @api_id = api_id
      @errors = {}
      @root_errors = Set.new
    end

    def validate_label(label)
      if label && label.length > 25
        @errors[:label] = 'Label must be 25 characters or fewer.'
        @root_errors << 'Invalid label.'
      elsif label.blank?
        @errors[:label] = "Label can't be blank."
        @root_errors << "Fields can't be blank."
      end
    end

    def handle_root_errors
      @root_errors << "Fields can't be blank." if @errors.values.any? { |e| e.include?("can't be blank.") }
      @errors[:root] = if @root_errors.size == 1
                         @root_errors.first
                       else
                         %(Errors:<ul>#{@root_errors.map { |e| "<li>#{e}</li>" }.join}</ul>)
                       end
    end

    def strip_carriage_returns(str)
      str&.gsub("\r", '')
    end
  end
end
