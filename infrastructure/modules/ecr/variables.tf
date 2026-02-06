variable "environment" {
  description = "Environment name (dev, staging)"
  type        = string
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "repository_names" {
  description = "List of ECR repository names to create"
  type        = list(string)
  default     = ["api", "ingestion", "workers"]
}
