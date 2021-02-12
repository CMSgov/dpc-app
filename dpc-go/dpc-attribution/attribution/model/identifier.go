package model

type Identifier struct {
	Value  string `json:"value"`
	System string `json:"system"`
}

type IdentifierContainer struct {
	Identifier []Identifier `json:"identifier"`
}
