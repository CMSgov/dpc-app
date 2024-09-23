# frozen_string_literal: true

# It Bars
class Bar
  def something_bar(check)
    return 'a' if check == 'a'

    'b'
  end

  def something_bar_else(check)
    return 'd' if check == 'd'

    'c'
  end

  def something_bar_third(check)
    return 'e' if check == 'e'

    'f'
  end
end
