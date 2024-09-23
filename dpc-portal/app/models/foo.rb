# frozen_string_literal: true

# It Foos
class Foo < ApplicationRecord
  def something(check)
    return 'a' if check == 'a'

    'b'
  end

  def something_else(check)
    return 'd' if check == 'd'

    'c'
  end

  def something_third(check)
    return 'e' if check == 'e'

    'f'
  end
end
