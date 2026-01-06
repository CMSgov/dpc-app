# app/jobs/memory_throttle_job.rb
class MemoryThrottleJob < ApplicationJob
  queue_as :default

  def perform(mb_to_allocate = 500)
    # Create a giant array of strings to eat up RAM
    # Each 'A' * 1.megabyte is roughly 1MB of heap
    @leak = []
    mb_to_allocate.times do
      @leak << "A" * 1_048_576 
    end
    
    Rails.logger.info "Allocated #{mb_to_allocate}MB. Current RSS: #{`ps -o rss= -p #{Process.pid}`.to_i / 1024}MB"
    
    # Keep the memory held for a bit to trigger the 5-minute alarm window
    sleep 60 
  end
end