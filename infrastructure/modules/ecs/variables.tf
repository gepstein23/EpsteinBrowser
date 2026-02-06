variable "environment" {
  description = "Environment name (dev, staging)"
  type        = string
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for Fargate tasks"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ALB security group ID for ingress rules"
  type        = string
}

variable "api_target_group_arn" {
  description = "ALB target group ARN for the API service"
  type        = string
}

variable "execution_role_arn" {
  description = "ECS task execution role ARN"
  type        = string
}

variable "api_task_role_arn" {
  description = "API task role ARN"
  type        = string
}

variable "ingestion_task_role_arn" {
  description = "Ingestion task role ARN"
  type        = string
}

variable "worker_task_role_arn" {
  description = "Worker task role ARN"
  type        = string
}

variable "api_image" {
  description = "API container image URI"
  type        = string
}

variable "ingestion_image" {
  description = "Ingestion container image URI"
  type        = string
}

variable "workers_image" {
  description = "Workers container image URI"
  type        = string
}

variable "api_log_group" {
  description = "CloudWatch log group for API"
  type        = string
}

variable "ingestion_log_group" {
  description = "CloudWatch log group for ingestion"
  type        = string
}

variable "workers_log_group" {
  description = "CloudWatch log group for workers"
  type        = string
}
