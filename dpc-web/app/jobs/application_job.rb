class ApplicationJob < ActiveJob::Base
  # Ensure jobs get sent to a specialized web queue. Our sandbox web applications share
  # a single solid queue database and process jobs based on their queue name.
  queue_as :web
end
