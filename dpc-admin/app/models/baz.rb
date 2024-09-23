# frozen_string_literal: true

# It Bazs
class Baz
  def something_baz(check)
    return 'a' if check == 'a'

    'b'
  end

  def something_baz_else(check)
    return 'd' if check == 'd'

    'c'
  end

  def something_baz_third(check)
    return 'e' if check == 'e'

    'f'
  end
end
