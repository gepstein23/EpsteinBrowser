variable "environment" {
  description = "Environment name (dev, staging)"
  type        = string
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "role_arn" {
  description = "IAM role ARN for the state machine"
  type        = string
}

variable "extract_text_task_arn" {
  description = "ARN of the extract text ECS task or Lambda"
  type        = string
}

variable "index_task_arn" {
  description = "ARN of the index document ECS task or Lambda"
  type        = string
}
