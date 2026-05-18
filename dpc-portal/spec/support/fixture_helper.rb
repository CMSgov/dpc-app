# frozen_string_literal: true

module FixtureHelper
  def file_fixture(path)
    File.read(Rails.root.join('spec', 'fixtures', path))
  end

  # Optional helper to immediately parse it into a Ruby Hash/Array
  def json_fixture(path)
    JSON.parse(file_fixture(path))
  end
end
