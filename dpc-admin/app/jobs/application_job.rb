# frozen_string_literal: true

class ApplicationJob < ActiveJob::Base
  # Ensure jobs get sent to a specialized admin queue. Our web applications share
  # a single Redis instance and process jobs based on their queue name.
  queue_as :admin

  # Automatically retry jobs that encountered a deadlock
  # retry_on ActiveRecord::Deadlocked

  # Most jobs are safe to ignore if the underlying records are no longer available
  # discard_on ActiveJob::DeserializationError
end
