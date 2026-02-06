variable "environment" {
  description = "Environment name (dev, staging)"
  type        = string
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "alert_email" {
  description = "Email address for alarm notifications"
  type        = string
  default     = ""
}
