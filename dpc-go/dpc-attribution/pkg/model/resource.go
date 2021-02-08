package model

import (
	"strconv"
	"time"
)

type Identifier struct {
	Value  string
	System string
}

type Resources struct {
	ID         string
	Version    int
	CreatedAt  time.Time
	UpdatedAt  time.Time
	Info       map[string]interface{}
	Identifier []Identifier
}

type Resource interface {
	VersionId() string
	LastUpdated() string
	ResourceType() string
}

func (o *Resources) ResourceType() string {
	return o.Info["resourceType"].(string)
}

func (o *Resources) VersionId() string {
	return strconv.Itoa(o.Version)
}

func (o *Resources) LastUpdated() string {
	return o.UpdatedAt.UTC().Format("2006-01-02T15:04:05.999-07:00")
}
