# frozen_string_literal: true

namespace :webpacker do
  task :check_npm do
    begin
      npm_version = `npm --version`
      raise Errno::ENOENT if npm_version.blank?

      version = Gem::Version.new(npm_version)
      package_json_path = Pathname.new("#{Rails.root}/package.json").realpath
      npm_requirement = JSON.parse(package_json_path.read).dig('engines', 'npm')
      requirement = Gem::Requirement.new(npm_requirement)
      unless requirement.satisfied_by?(version)
        warn "Webpacker requires npm #{requirement} and you are using #{version}" && exit!
      end
    rescue Errno::ENOENT
      warn 'npm not installed'
      warn 'Install NPM https://www.npmjs.com/get-npm' && exit!
    end
  end
  task :npm_install do
    system 'npm install'
  end
end
