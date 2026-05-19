# frozen_string_literal: true

module FixtureHelper
  def read_file(path)
    File.read(Rails.root.join('spec', 'fixtures', path))
  end

  # Optional helper to immediately parse it into a Ruby Hash/Array
  def json_fixture(path)
    JSON.parse(read_file(path))
  end
end
