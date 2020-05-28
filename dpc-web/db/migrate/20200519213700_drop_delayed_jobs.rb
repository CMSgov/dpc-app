require_relative '20190827134448_create_delayed_jobs'

class DropDelayedJobs < ActiveRecord::Migration[5.2]
  def change
    # revert allows using a previous migration to determine the up/down methods
    revert CreateDelayedJobs
  end
end
