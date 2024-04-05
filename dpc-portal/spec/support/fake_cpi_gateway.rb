require 'sinatra'
set :server, 'webrick'

get "/movies/:movie_name/actors" do
  {
    actors: [
      {
        name: "Actor 1",
        character_played: "Character 1"
      },
      {
        name: "Actor 2",
        character_played: "Character 2"
      }
    ]
  }.to_json
end
