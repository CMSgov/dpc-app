config.lograge.custom_options = lambda do |event|
  { :params => event.payload[:params],
    :level => event.payload[:level],
  }
end
