# frozen_string_literal: true

require 'selenium/webdriver'

Capybara.register_driver :selenium_headless do |app|
  options = Selenium::WebDriver::Firefox::Options.new(args: ['--headless'])
  service = Selenium::WebDriver::Service.firefox(path: '/usr/bin/geckodriver')

  Capybara::Selenium::Driver.new(
    app,
    browser: :firefox,
    options:,
    service:
  )
end
