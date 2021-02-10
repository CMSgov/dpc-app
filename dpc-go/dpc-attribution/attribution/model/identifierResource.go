package model

type Identifier struct {
	Value  string `json:"value"`
	System string `json:"system"`
}

type IdentifierResource struct {
	Identifier []Identifier
}
