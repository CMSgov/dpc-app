# frozen_string_literal: true

module DpcMiddleware
  class IgFix
    def initialize(app)
      @app = app
    end

    def call(env)
      request = Rack::Request.new(env)
      return @app.call(env) unless %r{^\/ig$}i.match?(request.path_info)

      url_parts = request.url.split('?')
      url_parts[0] += '/'

      [301, { 'Location' => url_parts.join('?') }, []]
    end
  end
end
