variable "environment" {
  description = "Environment name (dev, staging)"
  type        = string
}

variable "prefix" {
  description = "Resource name prefix"
  type        = string
}

variable "documents_bucket_arn" {
  description = "ARN of the S3 documents bucket"
  type        = string
}

variable "opensearch_domain_arn" {
  description = "ARN of the OpenSearch domain"
  type        = string
}

variable "extract_text_queue_arn" {
  description = "ARN of the extract text SQS queue"
  type        = string
}
