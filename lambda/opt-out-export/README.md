# How to run locally
- Make sure your local db is running.
    - In dpc-app docker-compose.yml, make sure port 5432 is open in the db service.
    - In the main dpc-app directory, run `make start-db`.
    - Check to make sure that the `patients` table in `dpc-attribution` and the `consent` table in `dpc-consent` have data.
    - If they don't, the easiest way to fill them is to run the dpc-api PatientResourceTest integration tests.
        - After they run the tables will be filled, but you'll have to modify a few MBIs to make sure patients you want are opted out.
- Make sure LocalStack is running.
    - From this directory, run `docker-compose up -d`.
    - If you haven't already, run `./populate_aws.sh`.
        - This script adds the necessary parameters to SSM and builds the S3 bucket that this lambda uses.
- At this point, you should be able to call `make run-local` to execute the process.
    - The alignment file will be written to your local file system along with the S3 bucket.
    - To see the contents of the S3 bucket run `aws --endpoint-url=http://localhost:4566 s3api list-objects --bucket bfd-local-eft`.
    - To download the file run `aws --endpoint-url=http://localhost:4566 s3api get-object --bucket bfd-local-eft --key bfdeft01/dpc/out/<FILE_NAME_HERE> file_from_s3.txt`.


<br><br>
# Running from within the VS Code debugger
- Follow the first two steps above to get the local DB and LocalStack running.
- In the debugger tab create a new launch package for the project.
    - In your `.vscode` directory, open up `launch.json`.
    - Most of the necessary fields are already filled in, you just need to add an `env` field with all of the environment variables our lambda needs.  They can be found in the `Makefile` under the `run-local` target.
    - Example `launch.json`:
    ```
    {
        // Use IntelliSense to learn about possible attributes.
        // Hover to view descriptions of existing attributes.
        // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
        "version": "0.2.0",
        "configurations": [
            
            {
                "name": "Launch Package",
                "type": "go",
                "request": "launch",
                "mode": "auto",
                "program": "${fileDirname}",
                "env": {
                    "ENV": "local",
                    "IS_TESTING": "true",
                    "LOCAL_STACK_ENDPOINT": "http://localhost:4566",
                    "DB_HOST": "localhost",
                    "S3_UPLOAD_BUCKET": "bfd-local-eft",
                    "S3_UPLOAD_PATH": "bfdeft01/dpc/out"
                }
            }
        ]
    }
    ```
    - Once this is set, you should be able to go into the debug tab and click the green triangle to run the lambda in debug mode.