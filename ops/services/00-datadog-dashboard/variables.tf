variable "env" {
  default     = "test"
  description = "The application environment; restricted to test in the case of the Datadog dashboard."
  type        = string
}
