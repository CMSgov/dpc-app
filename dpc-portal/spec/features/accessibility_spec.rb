# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', js: true, accessibility: true do
  base_url = 'localhost:3100'
  remote_url = 'selenium.cloud.cms.gov'
  options = Selenium::WebDriver::Chrome::Options
  driver = Selenium::WebDriver.for(:remote, url: remote_url, options:)
  context 'not signed in' do
    it '/sign_in' do
      WebMock.allow_net_connect!
      driver.visit "#{base_url}/portal/users/sign_in"
      expect(driver.page).to be_axe_clean
      WebMock.disable_net_connect!
    end
  end
end
