Rails.application.config.session_store :cache_store, key: '_dpc_app_session' unless Rails.env.local?
