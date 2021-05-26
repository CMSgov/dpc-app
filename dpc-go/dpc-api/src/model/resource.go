package model

import (
	"strconv"
	"time"
)

// Resource is struct for json marshalling of the attribution response
// with the only fields that a FHIR output cares about
type Resource struct {
	ID             string                 `json:"id" faker:"uuid_hyphenated"`
	Info           map[string]interface{} `json:"info" faker:"-"`
	Version        int                    `json:"version" faker:"-"`
	UpdatedAt      time.Time              `json:"updated_at" faker:"-"`
	OrganizationID *string                `json:"organizationId,omitempty" faker:"-"`
}

// ResourceType function to return the resource type of the underlying fhir model
func (r *Resource) ResourceType() string {
	return r.Info["resourceType"].(string)
}

// VersionID function to return the version id as a string
func (r *Resource) VersionID() string {
	return strconv.Itoa(r.Version)
}

// LastUpdated function to return the updatedAt field from attribution into a fhir date time format
func (r *Resource) LastUpdated() string {
	return r.UpdatedAt.UTC().Format("2006-01-02T15:04:05.999-07:00")
}
