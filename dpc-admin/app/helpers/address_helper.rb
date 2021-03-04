# frozen_string_literal: true

module AddressHelper
  def states_for_select
    Address::STATES.map { |abbrv, name| [name, abbrv.to_s] }
  end
end
