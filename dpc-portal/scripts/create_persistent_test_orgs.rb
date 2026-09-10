# frozen_string_literal: true

environment = ENV.fetch('ENV', nil)

invite_emails = {
  clear: {
    'dev' => 'ahackett@gmail.com',
    'test' => 'ahackett@gmail.com',
    'prod' => nil
  },
  login_dot_gov: {
    'dev' => 'luke+lowerenvtesting@verdance.co',
    'test' => 'luke+lowerenvtesting@verdance.co',
    'prod' => nil
  },
  id_me: {
    'dev' => 'luke+lowerenvtesting@verdance.co',
    'test' => 'luke+lowerenvtesting@verdance.co',
    'prod' => nil
  }
}

organizations = [
  {
    csp: :login_dot_gov,
    dpc_api_organization_id: 'a3abaf86-2cd4-4a32-a57e-1bda741ed00d',
    npi: '0009000122',
    name: 'Login.gov test org'
  },
  {
    csp: :id_me,
    dpc_api_organization_id: '66e9f10c-31c7-41a4-b88f-4d10e59432d7',
    npi: '0009000239',
    name: 'ID.me test org'
  },
  {
    csp: :clear,
    dpc_api_organization_id: '97509c9f-4350-4b4f-a9d4-aba4dadffe1c',
    npi: '0009000346',
    name: 'CLEAR test org'
  }
]

service = AoInvitationService.new

organizations.each do |organization_data|
  provider_organization = ProviderOrganization.find_or_create_by!(
    npi: organization_data[:npi],
    dpc_api_organization_id: organization_data[:dpc_api_organization_id]
  ) do |org|
    org.name = organization_data[:name]
  end

  puts "got provider organization #{provider_organization.id}, #{provider_organization.dpc_api_organization_id}"

  email = invite_emails.fetch(organization_data[:csp]).fetch(environment)
  if email.nil?
    puts "skipping #{organization_data[:csp]} persistent test org creation for ENV=#{environment.inspect}"
    next
  end

  invitation = service.create_invitation('Test', 'User', email, organization_data[:npi])
  puts "created AO invitation #{invitation.id} for #{email}"
  if Rails.env.development?
    puts "http://localhost:3100/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/accept"
  end
end
