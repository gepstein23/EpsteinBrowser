output "domain_endpoint" {
  description = "OpenSearch domain endpoint"
  value       = aws_opensearch_domain.main.endpoint
}

output "domain_arn" {
  description = "OpenSearch domain ARN"
  value       = aws_opensearch_domain.main.arn
}

output "security_group_id" {
  description = "OpenSearch security group ID"
  value       = aws_security_group.opensearch.id
}
