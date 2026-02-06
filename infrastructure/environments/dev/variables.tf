variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
  default     = "epstein-dev"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "aurora_master_password" {
  description = "Aurora master password"
  type        = string
  sensitive   = true
}

variable "alert_email" {
  description = "Email for alarm notifications"
  type        = string
  default     = ""
}
