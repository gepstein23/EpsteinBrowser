output "sns_topic_arn" {
  description = "SNS alerts topic ARN"
  value       = aws_sns_topic.alerts.arn
}

output "api_log_group_name" {
  description = "API CloudWatch log group name"
  value       = aws_cloudwatch_log_group.api.name
}

output "ingestion_log_group_name" {
  description = "Ingestion CloudWatch log group name"
  value       = aws_cloudwatch_log_group.ingestion.name
}

output "workers_log_group_name" {
  description = "Workers CloudWatch log group name"
  value       = aws_cloudwatch_log_group.workers.name
}
