# frozen_string_literal: true

module NavHelper
  def current_sidenav_class?(nav_item, current)
    'ds-c-vertical-nav__label--current' if nav_item.to_s == current.to_s
  end

  def current_sidenav_class
    'ds-c-vertical-nav__label--current'
  end

  def orgs
    'organizations'
  end

  def users
    'users'
  end

  def users_header(count)
    user_type = if unassigned_provider_users_page?
                  ' unassigned provider'
                elsif assigned_provider_users_page?
                  ' assigned provider'
                elsif unassigned_vendor_users_page?
                  ' unassigned vendor'
                elsif assigned_vendor_users_page?
                  ' assigned vendor'
                else
                  ''
                end
    ('Viewing ' + content_tag(:strong, count) + user_type + ' user' + plural_suffix(count)).html_safe
  end

  def orgs_header(count)
    org_type = if vendor_organizations_page?
                 ' vendor'
               elsif provider_organizations_page?
                 ' provider'
               else
                 ''
               end
    ('Viewing ' + content_tag(:strong, count) + org_type + ' organization' + plural_suffix(count)).html_safe
  end

  def plural_suffix(count)
    if count > 1 || count.zero?
      's'
    else
      ''
    end
  end

  def users_page?
    current_page?({ controller: users })
  end

  def provider_users_page?
    current_page?({ controller: users, org_type: 'provider' })
  end

  def vendor_users_page?
    current_page?({ controller: users, org_type: 'vendor' })
  end

  def organizations_page?
    current_page?({ controller: orgs })
  end

  def tags_page?
    current_page?({ controller: 'tags' })
  end

  def all_users_page?
    current_page?({ controller: users, org_type: 'all' })
  end

  def unassigned_vendor_users_page?
    current_page?({ controller: users, org_type: 'vendor', org_status: 'unassigned' })
  end

  def assigned_vendor_users_page?
    current_page?({ controller: users, org_type: 'vendor', org_status: 'assigned' })
  end

  def unassigned_provider_users_page?
    current_page?({ controller: users, org_type: 'provider', org_status: 'unassigned' })
  end

  def assigned_provider_users_page?
    current_page?({ controller: users, org_type: 'provider', org_status: 'assigned' })
  end

  def all_organizations_page?
    current_page?({ controller: orgs, org_type: 'all' })
  end

  def vendor_organizations_page?
    current_page?({ controller: orgs, org_type: 'vendor' })
  end

  def provider_organizations_page?
    current_page?({ controller: orgs, org_type: 'provider' })
  end
end
