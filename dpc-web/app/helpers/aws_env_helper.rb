# frozen_string_literal: true

module AwsEnvHelper
  def prod_sbx?
    ENV['AWS_ENV'] == 'prod-sbx'
  end
end
