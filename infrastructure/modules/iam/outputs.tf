output "ecs_execution_role_arn" {
  description = "ECS task execution role ARN"
  value       = aws_iam_role.ecs_execution.arn
}

output "api_task_role_arn" {
  description = "API task role ARN"
  value       = aws_iam_role.api_task.arn
}

output "ingestion_task_role_arn" {
  description = "Ingestion task role ARN"
  value       = aws_iam_role.ingestion_task.arn
}

output "worker_task_role_arn" {
  description = "Worker task role ARN"
  value       = aws_iam_role.worker_task.arn
}

output "step_functions_role_arn" {
  description = "Step Functions execution role ARN"
  value       = aws_iam_role.step_functions.arn
}
