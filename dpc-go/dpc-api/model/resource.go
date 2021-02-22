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
	ID         string                 `json:"id"`
	Version    int                    `json:"version"`
	CreatedAt  time.Time              `json:"created_at"`
	UpdatedAt  time.Time              `json:"updated_at"`
	Info       map[string]interface{} `json:"info"`
	Identifier []Identifier           `json:"identifier"`
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
