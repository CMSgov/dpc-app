# DPC Implementer Portal README

This is the implementer portal for Data at the Point of Care (DPC) 2.0. Through this portal, users can create accounts for themselves 

## Before Installation

Follow instructions to set up version 2.0 of the DPC API. Make sure the API is running locally by visiting `http://localhost:3000/v2/metadata`. If you see a JSON object, it is running.

## Install Requirements

This is a Ruby on Rails driven website with a postgreSQL database. You will need to install ruby and postgres locally to run this application on your computer.

**Make sure that the postgres port is set to 5434.** The API is using the default 5432. The ruby app is automatically set to 5434, but you will have to change the postgres ports locally.

## Installation and Configuration of the Application