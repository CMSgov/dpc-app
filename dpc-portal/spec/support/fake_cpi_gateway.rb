require 'sinatra'
set :server, 'webrick'
enable  :logging

post "/api/1.0/ppr/providers/enrollments" do
  headers["content-type"] = "application/json; charset=UTF-8"
  {
    enrollments: [
      {
        status: 'APPROVED',
        enrollmentID: 'WTVR'
      }
    ]
  }.to_json
end

get "/api/1.0/ppr/providers/enrollments/:enrollmentID/roles" do
  headers["content-type"] = "application/json; charset=UTF-8"
  {
    enrollments: {
      roles: [
        {
          pacId: "900111111",
          roleCode: "10",
          ssn: "900111111"
        }
      ]
    }
  }.to_json
end

post "/api/1.0/ppr/providers" do
  headers["content-type"] = "application/json; charset=UTF-8"
  {
    provider: {}
  }.to_json
end
