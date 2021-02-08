package model

import (
	"strconv"
	"time"
)

type Identifier struct {
	Value  string `json:"value"`
	System string `json:"system"`
}

type Resource struct {
	ID         string
	Version    int
	CreatedAt  time.Time
	UpdatedAt  time.Time
	Info       map[string]interface{}
	Identifier []Identifier
}

type FhirResource interface {
	VersionId() string
	LastUpdated() string
	ResourceType() string
}

func (r *Resource) ResourceType() string {
	return r.Info["resourceType"].(string)
}

func (r *Resource) VersionId() string {
	return strconv.Itoa(r.Version)
}

func (r *Resource) LastUpdated() string {
	return r.UpdatedAt.UTC().Format("2006-01-02T15:04:05.999-07:00")
}
