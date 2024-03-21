class ApplicationJob < ActiveJob::Base
  # Ensure jobs get sent to a specialized web queue. Our web applications share
  # a single Redis instance and process jobs based on their queue name.
  queue_as :web
end
