output "queue_url" {
  description = "Extract text queue URL"
  value       = aws_sqs_queue.extract_text.url
}

output "queue_arn" {
  description = "Extract text queue ARN"
  value       = aws_sqs_queue.extract_text.arn
}

output "dlq_url" {
  description = "Dead letter queue URL"
  value       = aws_sqs_queue.extract_text_dlq.url
}

output "dlq_arn" {
  description = "Dead letter queue ARN"
  value       = aws_sqs_queue.extract_text_dlq.arn
}
