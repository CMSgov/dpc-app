# frozen_string_literal: true

module CsvGenerator

  ATTRS = %w[id first_name last_name email requested_organization requested_organization_type
    address_1 address_2 city state zip agree_to_terms requested_num_providers created_at updated_at].freeze

  # html escape these fields for XSS protection
  ESCAPED_ATTRS = %w[first_name last_name requested_organization address_1 address_2 city].freeze

  def cvs_convert(users)
    CSV.generate(headers:true) do |csv|
      csv << ATTRS
      users.each do |user|
        attributes = user.attributes
        escaped_attributes = attributes.map do |k, v|
          if ESCAPED_ATTRS.include? k
            v = ERB::Util.html_escape(v)
          end

          [k, v]
        end.to_h
        csv << escaped_attributes.values_at(*ATTRS)
      end
    end
  end

end
