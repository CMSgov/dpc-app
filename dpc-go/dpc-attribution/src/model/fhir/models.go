// Package fhir contains structs representing FHIR data.
// These data models are a lighter weight definition contain certain fields needed for DPC
package fhir

import "time"

// Resource models a FHIR resource
type Resource struct {
	ResourceType string `json:"resourceType"`
	ID           string `json:"id"`
	Meta         struct {
		LastUpdated time.Time `json:"lastUpdated"`
	} `json:"meta"`
	Total uint `json:"total"`
}

// Bundle models a FHIR bundle
type Bundle struct {
	Resource
	Links []struct {
		Relation string `json:"relation"`
		URL      string `json:"url"`
	} `json:"link"`
	Entries []BundleEntry `json:"entry"`
}

// BundleEntry models a FHIR bundle entry
type BundleEntry map[string]interface{}
