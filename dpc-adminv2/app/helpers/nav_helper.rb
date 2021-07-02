# frozen_string_literal: true

module NavHelper
  def current_navadmin_class
    'navadmin--current'
  end

  def home_page?
    current_page?('/adminv2')
  end

  def implementers_page?
    current_page?({ controller: 'implementers' })
  end

  def provider_orgs_page?
    current_page?({ controller: 'provider_orgs' })
  end

  def users_page?
    current_page?({ controller: 'users' })
  end
end
