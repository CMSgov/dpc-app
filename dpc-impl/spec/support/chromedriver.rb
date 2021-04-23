# frozen_string_literal: true

require 'selenium/webdriver'

Capybara.javascript_driver = :selenium_chrome_headless

Capybara.register_driver :selenium do |app|
  Capybara::Selenium::Driver.new(app, browser: :chrome)
end
