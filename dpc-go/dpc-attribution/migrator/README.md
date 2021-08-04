###Creating and Running migrations using golan-migrate/migrate

####Creating a new Migration
Each migration consists of two files, an up file containing the new change (as a sql query) and down file containing the rollback query.

1) Create up down files and place them in the `/migrations` directory.  
    Files should be in the following format:  
    {id}.{migration_name}.up.sql  
    {id}.{migration_name}.down.sql
    

`id` should be incrementing and numerical. the up and down files for a single migration should have the same id.
`migration_name` is not used by the migrator and only used as a friendly name.  

Example:  
Assuming the last migration in the `/migrations` folder has an id of 41 a new migration could look like this:
`42_ADD_EMAIL_COL.up.sql`  
`42_ADD_EMAIL_COL.down.sql` 


####Running Migrations
1) Build the docker image (uses the `Dockerfile` found in this directory) that will be used to run the migration commands.
`docker build -t migrator .`

*Note: The `Dockerfile` copies the files under /migrations to the image's own volume.*

2) Run the migrations: 
 `docker run --network host migrator -path=/migrations/ -database 'postgres://postgres:dpc-safe@localhost:5432/dpc_attribution_v2?sslmode=disable' up`
 
3) Rollback a migration: 
  `docker run --network host migrator -path=/migrations/ -database 'postgres://postgres:dpc-safe@localhost:5432/dpc_attribution_v2?sslmode=disable' down 4`
  
    * *You may roll back a specific migration by specifying the id*
  
####Checking Schema Version
The table `shema_migrations` contains the most recent migration and its status.